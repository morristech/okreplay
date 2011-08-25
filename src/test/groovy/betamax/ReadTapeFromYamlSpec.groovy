package betamax

import betamax.storage.yaml.YamlTapeLoader
import groovy.json.JsonException
import org.codehaus.groovy.runtime.typehandling.GroovyCastException
import betamax.storage.*
import static java.net.HttpURLConnection.HTTP_OK
import static org.apache.http.HttpHeaders.*
import static org.apache.http.HttpVersion.HTTP_1_1
import spock.lang.*
import org.yaml.snakeyaml.constructor.ConstructorException

class ReadTapeFromYamlSpec extends Specification {

	TapeLoader loader = new YamlTapeLoader()

	def "can load a valid tape with a single interaction"() {
		given:
		def yaml = """\
!tape
name: single_interaction_tape
interactions:
- recorded: 2011-08-23T22:41:40.000Z
  request:
    protocol: HTTP/1.1
    method: GET
    uri: http://icanhascheezburger.com/
    headers: {Accept-Language: 'en-GB,en', If-None-Match: b00b135}
  response:
    protocol: HTTP/1.1
    status: 200
    headers: {Content-Type: text/plain, Content-Language: en-GB}
    body: O HAI!
"""
		when:
		def tape = loader.readTape(new StringReader(yaml))

		then:
		tape.name == "single_interaction_tape"
		tape.interactions.size() == 1
		tape.interactions[0].recorded == new Date(111, 7, 23, 23, 41, 40)
		tape.interactions[0].request.protocol == HTTP_1_1.toString()
		tape.interactions[0].request.method == "GET"
		tape.interactions[0].request.uri == "http://icanhascheezburger.com/"
		tape.interactions[0].response.protocol == HTTP_1_1.toString()
		tape.interactions[0].response.status == HTTP_OK
		tape.interactions[0].response.headers[CONTENT_TYPE] == "text/plain"
		tape.interactions[0].response.headers[CONTENT_LANGUAGE] == "en-GB"
		tape.interactions[0].response.body == "O HAI!"
	}

	def "can load a valid tape with multiple interactions"() {
		given:
		def yaml = """\
!tape
name: multiple_interaction_tape
interactions:
- recorded: 2011-08-23T23:41:40.000Z
  request:
    protocol: HTTP/1.1
    method: GET
    uri: http://icanhascheezburger.com/
    headers: {Accept-Language: 'en-GB,en', If-None-Match: b00b135}
  response:
    protocol: HTTP/1.1
    status: 200
    headers: {Content-Type: text/plain, Content-Language: en-GB}
    body: O HAI!
- recorded: 2011-08-23T23:41:40.000Z
  request:
    protocol: HTTP/1.1
    method: GET
    uri: http://en.wikipedia.org/wiki/Hyper_Text_Coffee_Pot_Control_Protocol
    headers: {Accept-Language: 'en-GB,en', If-None-Match: b00b135}
  response:
    protocol: HTTP/1.1
    status: 418
    headers: {Content-Type: text/plain, Content-Language: en-GB}
    body: I'm a teapot
"""
		when:
		def tape = loader.readTape(new StringReader(yaml))

		then:
		tape.interactions.size() == 2
		tape.interactions[0].request.uri == "http://icanhascheezburger.com/"
		tape.interactions[1].request.uri == "http://en.wikipedia.org/wiki/Hyper_Text_Coffee_Pot_Control_Protocol"
		tape.interactions[0].response.status == HTTP_OK
		tape.interactions[1].response.status == 418
		tape.interactions[0].response.body == "O HAI!"
		tape.interactions[1].response.body == "I'm a teapot"
	}

	def "reads request headers"() {
		given:
		def yaml = """\
!tape
name: single_interaction_tape
interactions:
- recorded: 2011-08-23T22:41:40.000Z
  request:
    protocol: HTTP/1.1
    method: GET
    uri: http://icanhascheezburger.com/
    headers: {Accept-Language: 'en-GB,en', If-None-Match: b00b135}
  response:
    protocol: HTTP/1.1
    status: 200
    headers: {Content-Type: text/plain, Content-Language: en-GB}
    body: O HAI!
"""
		when:
		def tape = loader.readTape(new StringReader(yaml))

		then:
		tape.interactions[0].request.headers[ACCEPT_LANGUAGE] == "en-GB,en"
		tape.interactions[0].request.headers[IF_NONE_MATCH] == "b00b135"
	}

	def "barfs on non-yaml data"() {
		given:
		def yaml = "THIS IS NOT YAML"

		when:
		loader.readTape(new StringReader(yaml))

		then:
		thrown TapeLoadException
	}

	def "barfs on an invalid record date"() {
		given:
		def yaml = """\
!tape
name: invalid_date_tape
interactions:
- recorded: THIS IS NOT A DATE!
  request:
    protocol: HTTP/1.1
    method: GET
    uri: http://icanhascheezburger.com/
    headers: {Accept-Language: 'en-GB,en', If-None-Match: b00b135}
  response:
    protocol: HTTP/1.1
    status: 200
    headers: {Content-Type: text/plain, Content-Language: en-GB}
    body: O HAI!
"""
		when:
		loader.readTape(new StringReader(yaml))

		then:
		def e = thrown(TapeLoadException)
		e.cause instanceof ConstructorException
	}

	def "barfs on missing fields"() {
		given:
		def json = """\
!tape
name: missing_response_status_tape
interactions:
- recorded: 2011-08-23T23:41:40.000Z
  request:
    protocol: HTTP/1.1
    method: GET
    uri: http://icanhascheezburger.com/
    headers: {Accept-Language: 'en-GB,en', If-None-Match: b00b135}
  response:
    protocol: HTTP/1.1
    headers: {Content-Type: text/plain, Content-Language: en-GB}
    body: O HAI!
"""
		when:
		loader.readTape(new StringReader(json))

		then:
		def e = thrown(TapeLoadException)
		e.message == "Missing element 'status'"
	}

	def "barfs if no tape at root"() {
		given:
		def json = """\
name: invalid_structure_tape
"""
		when:
		loader.readTape(new StringReader(json))

		then:
		def e = thrown(TapeLoadException)
		e.message =~ /^Expected a Tape but loaded a /
	}

}