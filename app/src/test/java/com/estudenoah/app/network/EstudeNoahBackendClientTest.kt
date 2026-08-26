package com.estudenoah.app.network

import com.estudenoah.app.domain.Subject
import com.estudenoah.app.security.BackendLoginRequiredException
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class EstudeNoahBackendClientTest {
    @Test fun tokenIsObtained() = runBlocking { val f = fixture(okVf()); f.client.fromText("pdf", "A", "Ciências", DEFAULT_GRADE, text()); assertEquals(listOf(false), f.tokens.calls) }
    @Test fun headerIsAttached() = runBlocking { val f = fixture(okVf()); f.client.fromText("pdf", "A", "Ciências", DEFAULT_GRADE, text()); assertEquals("Bearer secret-token", f.transport.requests.single().headers[AUTHORIZATION_HEADER]) }
    @Test fun tokenDoesNotAppearInErrors() = runBlocking { val f = fixture(error(500)); val e = expectBackend { f.client.fromText("pdf", "A", "Ciências", DEFAULT_GRADE, text()) }; assertFalse(e.message.orEmpty().contains("secret-token")) }
    @Test fun firebaseAuthErrorIsFriendly() = runBlocking { val f = fixture(error(401, "firebase_auth_token_missing")); val e = expectBackend { f.client.fromText("pdf", "A", "Ciências", DEFAULT_GRADE, text()) }; assertEquals("A sessão da Conta do backend expirou. Peça ao responsável para entrar novamente.", e.userMessage()) }
    @Test fun unauthenticatedUserGetsFriendlyError() = runBlocking { val transport=FakeTransport(mutableListOf()); val client=EstudeNoahBackendClient(AuthTokenSource { throw BackendLoginRequiredException() },transport); val e=expectBackend { client.fromText("text","A","Português",DEFAULT_GRADE,text()) }; assertEquals("Peça ao responsável para conectar a Conta do backend na Área dos Pais.",e.userMessage()); assertTrue(transport.requests.isEmpty()) }
    @Test fun invalidTokenRetriesOnceWithRefresh() = runBlocking { val f = fixture(error(401, "firebase_auth_token_invalid"), okVf()); f.client.fromText("pdf", "A", "Ciências", DEFAULT_GRADE, text()); assertEquals(listOf(false, true), f.tokens.calls); assertEquals(2, f.transport.requests.size) }
    @Test fun invalidTokenNeverLoops() = runBlocking { val f = fixture(error(401, "firebase_auth_token_invalid"), error(401, "firebase_auth_token_invalid")); expectBackend { f.client.fromText("pdf", "A", "Ciências", DEFAULT_GRADE, text()) }; assertEquals(2, f.transport.requests.size) }
    @Test fun fromTextUsesExactJsonContract() = runBlocking { val f = fixture(okVf()); f.client.fromText("docx", "Título", "Português", "4º Ano", text()); val j=JSONObject(f.transport.requests.single().body.toString(Charsets.UTF_8)); assertEquals("docx",j.getString("sourceType")); assertEquals("Título",j.getString("sourceTitle")); assertEquals("Português",j.getString("subject")); assertEquals("4º Ano",j.getString("grade")); assertEquals(text(),j.getString("text")) }
    @Test fun pdfSourceTypeIsPreserved() = sourceType("pdf")
    @Test fun pptxSourceTypeIsPreserved() = sourceType("pptx")
    @Test fun docSourceTypeIsPreserved() = sourceType("doc")
    @Test fun docxSourceTypeIsPreserved() = sourceType("docx")
    @Test fun odtSourceTypeIsPreserved() = sourceType("odt")
    @Test fun trueFalseResponseMaps() { val q=BackendActivityMapper.questions(parse(okVf())); assertEquals(listOf("Verdadeiro","Falso"),q.single().options); assertEquals(0,q.single().correctIndex) }
    @Test fun falseAnswerMapsToSecondOption() { val q=BackendActivityMapper.questions(parse(okVf(answer=false))); assertEquals(1,q.single().correctIndex) }
    @Test fun mathResponseMapsStructuredAnswerWithoutInventingDistractors() { val q=BackendActivityMapper.questions(parse(okMath())).single(); assertTrue(q.options.isEmpty()); assertEquals("42",q.mathAnswer); assertEquals(listOf("6 × 7 = 42"),q.solutionSteps); assertEquals("Multiplique.",q.explanation); assertTrue(q.isMathProblem) }
    @Test fun backendSubjectMapsToAndroid() { assertEquals(Subject.MATEMATICA, BackendActivityMapper.subject("Matemática")); assertEquals(Subject.CIENCIAS, BackendActivityMapper.subject("Ciências")) }
    @Test fun error413Maps() = statusMessage(413,"O material é grande demais para ser analisado.")
    @Test fun error422Maps() = statusMessage(422,"Não foi possível obter conteúdo suficiente deste material.")
    @Test fun error429Maps() = statusMessage(429,"Há muitas solicitações no momento. Aguarde um pouco e tente novamente.")
    @Test fun error500Maps() = statusMessage(500,"O serviço está temporariamente indisponível. Tente novamente.")
    @Test fun invalidJsonIsRejected() = runBlocking { val f=fixture(BackendResponse(200,"not-json")); val e=expectBackend { f.client.fromText("text","A","Português",DEFAULT_GRADE,text()) }; assertEquals("incompatible_response",e.code) }
    @Test fun pptUsesExactMultipartContract() = runBlocking { val f=fixture(okVf()); f.client.fromPpt("aula.ppt",byteArrayOf(1,2,3),"História",DEFAULT_GRADE); val r=f.transport.requests.single(); val body=r.body.toString(Charsets.UTF_8); assertEquals("/v1/activities/from-ppt",r.path); assertTrue(r.contentType.startsWith("multipart/form-data; boundary=")); assertTrue(body.contains("name=\"file\"; filename=\"aula.ppt\"")); assertTrue(body.contains("name=\"subject\"\r\n\r\nHistória")); assertTrue(body.contains("name=\"grade\"\r\n\r\n$DEFAULT_GRADE")); assertTrue(r.body.contains(1.toByte())) }
    @Test fun ppsPreservesMultipartBytesFilenameAndBearer() = runBlocking { val bytes=byteArrayOf(7,0,42,99); val f=fixture(okVf()); f.client.fromPpt("Aula Final.pps",bytes,"História",DEFAULT_GRADE); val r=f.transport.requests.single(); val body=r.body.toString(Charsets.ISO_8859_1); assertEquals("/v1/activities/from-ppt",r.path); assertTrue(body.contains("filename=\"Aula Final.pps\"")); assertTrue(r.body.asList().windowed(bytes.size).any { it == bytes.asList() }); assertEquals("Bearer secret-token",r.headers[AUTHORIZATION_HEADER]) }
    @Test fun youtubeUsesAnalysisThenGenerationContracts() = runBlocking { val f=fixture(youtubeAnalysis(),okVf()); f.client.fromYoutube("https://youtu.be/abc123","Geografia",DEFAULT_GRADE); assertEquals(listOf("/v1/materials/youtube/analyze","/v1/activities/generate"),f.transport.requests.map{it.path}); val first=JSONObject(f.transport.requests[0].body.toString(Charsets.UTF_8)); assertEquals("https://youtu.be/abc123",first.getString("url")); val second=JSONObject(f.transport.requests[1].body.toString(Charsets.UTF_8)); assertEquals("youtube",second.getJSONObject("source").getString("type")); assertTrue(second.getJSONObject("analysis").getJSONArray("themes").length()>0) }
    @Test fun youtubeFailureDoesNotCallGeneration() = runBlocking { val f=fixture(error(500)); expectBackend { f.client.fromYoutube("https://youtu.be/abc123","Geografia",DEFAULT_GRADE) }; assertEquals(1,f.transport.requests.size) }

    private fun sourceType(type:String)=runBlocking { val f=fixture(okVf()); f.client.fromText(type,"A","Português",DEFAULT_GRADE,text()); assertEquals(type,JSONObject(f.transport.requests.single().body.toString(Charsets.UTF_8)).getString("sourceType")) }
    private fun statusMessage(status:Int,message:String)=runBlocking { val f=fixture(error(status)); val e=expectBackend { f.client.fromText("text","A","Português",DEFAULT_GRADE,text()) }; assertEquals(message,e.userMessage()) }
    private suspend fun expectBackend(block:suspend()->Unit):BackendException { try { block(); fail("Expected BackendException") } catch(e:BackendException){ return e }; throw AssertionError() }
    private fun parse(response:BackendResponse):BackendGeneratedActivity = runBlocking { fixture(response).client.fromText("text","A","Português",DEFAULT_GRADE,text()) }
    private fun fixture(vararg responses:BackendResponse):Fixture { val t=FakeTransport(responses.toMutableList()); val tokens=FakeTokens(); return Fixture(EstudeNoahBackendClient(tokens,t),t,tokens) }
    private data class Fixture(val client:EstudeNoahBackendClient,val transport:FakeTransport,val tokens:FakeTokens)
    private class FakeTokens:AuthTokenSource { val calls=mutableListOf<Boolean>(); override suspend fun token(forceRefresh:Boolean):String { calls+=forceRefresh; return if(forceRefresh) "fresh-token" else "secret-token" } }
    private class FakeTransport(private val responses:MutableList<BackendResponse>):BackendTransport { val requests=mutableListOf<BackendRequest>(); override suspend fun execute(request:BackendRequest):BackendResponse { requests+=request; return responses.removeAt(0) } }
    private fun error(status:Int,code:String="error")=BackendResponse(status,"{\"code\":\"$code\",\"message\":\"safe\"}")
    private fun text()="conteúdo pedagógico suficiente ".repeat(8)
    private fun okVf(answer:Boolean=true)=BackendResponse(200,"""{"subject":"Ciências","grade":"4º Ano","activityType":"TRUE_FALSE","themes":[{"name":"Tema","questions":[{"statement":"A água muda de estado.","answer":$answer,"explanation":"Explicação","evidence":["Fonte"],"theme":"Tema","learningObjective":"Compreender","difficulty":"medium","problem":null,"mathAnswer":null,"solutionSteps":null,"skill":null,"cognitiveDemand":"application","constructionType":"application"}]}],"warnings":[]}""")
    private fun okMath()=BackendResponse(200,"""{"subject":"Matemática","grade":"4º Ano","activityType":"MATH_PROBLEMS","themes":[{"name":"Multiplicação","questions":[{"statement":null,"answer":null,"explanation":"Multiplique.","evidence":[],"theme":"Multiplicação","learningObjective":"Resolver","difficulty":"medium","problem":"Quanto é 6 vezes 7?","mathAnswer":"42","solutionSteps":["6 × 7 = 42"],"skill":"multiplicação","cognitiveDemand":null,"constructionType":null}]}],"warnings":[]}""")
    private fun youtubeAnalysis()=BackendResponse(200,"""{"sourceType":"youtube","sourceUrl":"https://youtu.be/abc123","videoTitle":"Aula","subject":"Geografia","summary":"Resumo","themes":[{"name":"Tema","learningObjectives":["Objetivo"],"concepts":["Conceito"],"relationships":["Relação"],"likelyMisconceptions":["Erro"],"evidence":[{"description":"Trecho","timestamp":"00:10"}]}],"warnings":[]}""")
}

