import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.IOUtils;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;

public class ExemploIntegracaoGovBr {

	/**
	 * O processo de autentica��o e autoriza��o de recursos ocorre essencialmente em
	 * tr�s etapas: Etapa 1: Chamada do servi�o de autoriza��o do Gov.br; Etapa 2:
	 * Recupera��o do Access Token e Etapa 3: Valida��o do Access Token por meio da
	 * verifica��o de sua assinatura. Ap�s conclu�da essas tr�s etapas, a aplica��o
	 * cliente ter� as informa��es b�sicas para conceder acesso de acordo com suas
	 * pr�prias pol�ticas de autoriza��o. Caso a aplica��o cliente necessite de
	 * informa��es adicionais, fica habilitado o acesso � todos os servi�os
	 * (presentes e futuros) fornecidos pelo Gov.br por meio do access token. O
	 * presente c�digo exemplifica a chamada aos seguintes servi�os: getUserInfo -
	 * Extra��o das informa��es b�sicas de usu�rio atrav�s do ID Token; Servi�o 1:
	 * getFoto - Servi�o que recupera a foto do usu�rio; Servi�o 2: getNiveis -
	 * Servi�o que recupera os niveis do cidad�o Servi�o 3: getCategorias - Servi�o
	 * que recupera as categorias do cidad�o Servi�o 4: getConfiabilidade - Servi�o
	 * que recupera os selos de confiabilidade atribuidos ao usu�rio; Servi�o 5:
	 * getEmpresasVinculadas - Servi�o que recupera a lista de empresas vinculadas
	 * ao usu�rio; Servi�o 5: getDadosEmpresa - Servi�o que detalha a empresa e o
	 * papel do usu�rio nesta empresa. C�digo termina com chamada do Cat�logo de
	 * Confiabildides.
	 *
	 * *************************************************************************************************
	 *
	 * Informa��es de uso ------------------ Atribua �s vari�veis abaixo os valores
	 * de acordo com o seu sistema.
	 *
	 */

	private static final String URL_PROVIDER = "https://sso.staging.acesso.gov.br";
	private static final String URL_SERVICOS = "https://api.staging.acesso.gov.br";
	private static final String URL_CATALOGO_SELOS = "https://confiabilidades.staging.acesso.gov.br";
	private static final String REDIRECT_URI = "<coloque-aqui-url-de-retorno>"; // redirectURI informada na chamada do servi�o do
																		// authorize.
	private static final String SCOPES = "openid+(email/phone)+profile+govbr_empresa+govbr_confiabilidades"; // Escopos
																												// pedidos
																												// para
																												// a
																												// aplica��o.
	private static final String CLIENT_ID = "<coloque-aqui-o-client-id>"; // clientId
																										// informado na
																										// chamada do
																										// servi�o do
																										// authorize.
	private static final String SECRET = "<coloque-aqui-o-secret>"; // secret de
																									// conhecimento
																									// apenas do backend
																									// da aplica��o.
	private static final String NIVEIS = "<coloque-aqui-os-niveis-repeitando-sintaxe-virgula-barra-parenteses-segundo-roteiro>";
	private static final String CATEGORIAS = "<coloque-aqui-as-categorias-repeitando-sintaxe-virgula-barra-parenteses-segundo-roteiro>";
	private static final String CONFIABILIDADES = "<coloque-aqui-as-confiabilidades-repeitando-sintaxe-virgula-barra-parenteses-segundo-roteiro>";

	public static void main(String[] args) throws Exception {

		/**
		 * Etapa 1: No Browser, chamar a URL do Authorize para recuperar o code e o
		 * state (opcional) conforme o exemplo abaixo:
		 * https://sso.staging.acesso.gov.br/authorize?response_type=code&client_id=<coloque-aqui-o-client-id>&scope=openid+profile+(phone/email)+govbr_empresa&redirect_uri=<coloque-aqui-a-uri-de-redirecionamento>&nonce=<coloque-aqui-um-numero-aleatorio>&state=<coloque-aqui-um-numero-aleatorio>
		 * Descri��o dos parametros: response_type: Sempre "code"; client_id:
		 * Identificador do sistema que usa o Gov.br. Este identificador � �nico para
		 * cada sistema; scope: Lista de escopos requisitados pelo sistema. Escopos s�o
		 * agrupamentos de informa��es cujo acesso dever� ser autorizado pelo cidad�o
		 * que acessa o sistema. Cada sistema dever� informar que conjunto de
		 * informa��es (escopos) deseja; redirect_uri: Uri para qual ser� feito o
		 * redirect ap�s o login do cidad�o (usu�rio). Para Celulares, usamos uma pseudo
		 * URI; nonce: n�mero aleat�rio; state: n�mero aleat�rio (opcional)
		 *
		 * Observa��o: Sem o escopo "govbr_empresa", n�o ser� poss�vel utilizar o
		 * servi�o de recupera��o de informa��es de empresas.
		 */

		System.out.println("--------------------Etapa 1 - URL do Servi�o Authorize------------------");
		System.out.println("Abra um Browser (Chrome ou Firefox), aperte F12. Clique na aba 'Network'.");
		System.out.println(
				"Cole a URL abaixo no Browser (Chrome ou Firefox) e entre com um usu�rio cadastrado no Gov.br");
		System.out.println(URL_PROVIDER + "/authorize?response_type=code&client_id=" + CLIENT_ID + "&scope=" + SCOPES
				+ "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, "UTF-8") + "&nonce=" + createRandomNumber()
				+ "&state=" + createRandomNumber());

		/**
		 * Etapa 2: De posse do code retornado pelo passo 1, chame o servi�o para
		 * recuperar os tokens dispon�veis para sua aplica��o (Access Token, Id Token)
		 * conforme o exemplo abaixo.
		 */

		System.out.println("\n--------------------Etapa 2 - Recupera��o dos Tokens de Acesso------------------");
		System.out.println("Digite abaixo o par�metro 'code' retornado pelo redirect da etapa 1");
		System.out.print("Digite o valor do par�metro code retornado:");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String code = br.readLine();

		String tokens = extractToken(code);
		System.out.println("JSON retornado:");
		System.out.println(tokens);

		JSONParser parser = new JSONParser();
		JSONObject tokensJson = (JSONObject) parser.parse(tokens);

		String accessToken = (String) tokensJson.get("access_token");
		String idToken = (String) tokensJson.get("id_token");

		/**
		 * Etapa 3: De posse do access token, podemos extrair algumas informa��es acerca
		 * do usu�rio. Aproveitamos tamb�m para checar a assinatura e tempo de expira��o
		 * do token. Para isso, este exemplo usa a biblioteca Open Source chamada
		 * "jose4j" mas qualquer outra biblioteca que implemente a especifica��o pode
		 * ser usada.
		 *
		 * O Access Token fornece as seguintes informa��es acerca do usu�rio: 1- id
		 * client da aplica��o � qual o usu�rio se autenticou; 2- Escopos requeridos
		 * pela aplica��o autorizados pelo usu�rio; 3- CPF do usu�rio autenticado 4-
		 * Nome completo do usu�rio cadastrado no Gov.br. Aten��o, este � o nome que foi
		 * fornecido pelo usu�rio no momento do seu cadastro
		 *
		 */

		JwtClaims accessTokenJwtClaims;
		JwtClaims idTokenJwtClaims;
		try {
			accessTokenJwtClaims = processToClaims(accessToken);
			idTokenJwtClaims = processToClaims(idToken);
		} catch (Exception e) {
			System.out.println("Access Token inv�lido!");
			throw new Exception(e);
		}

		String idClient = accessTokenJwtClaims.getAudience().get(0); // Client Id
		List<String> scopes = accessTokenJwtClaims.getStringListClaimValue("scope"); // Escopos autorizados pelo usu�rio
		String nomeCompleto = idTokenJwtClaims.getStringClaimValue("name"); // Nome Completo do cadastro feito pelo
																			// usu�rio no Gov.br.
		String fotoUrl = idTokenJwtClaims.getStringClaimValue("picture"); //

		System.out.println("\n--------------------Etapa 3 - Informa��es obtidas do Access Token------------------");
		System.out.printf("O usu�rio " + nomeCompleto + " foi autenticado pelo Gov.br para usar o sistema " + idClient
				+ ". Este usu�rio tamb�m autorizou este mesmo sistema � utilizar as informa��es representadas pelos escopos "
				+ String.join(",", scopes) + ". \n");

		/**
		 * De posse do token de resposta, a aplica��o pode usar o id token para extrair
		 * as informa��es do usu�rio.
		 *
		 */

		System.out
				.println("\n--------------------Informa��es do usu�rio obtidas atrav�s do ID Token------------------");
		System.out.println("JSON retornado (base 64):");
		System.out.println(idToken);
		System.out.println("\n\nDados do usu�rio:");
		System.out.println("CPF: " + idTokenJwtClaims.getSubject()); // CPF do usu�rio autenticado.
		System.out.println("Nome: " + nomeCompleto); // Nome Completo do cadastro feito pelo usu�rio no Gov.br.
		System.out.println("Email Validado: " + idTokenJwtClaims.getClaimValue("email_verified")); // (Confirma se o
																									// email foi
																									// validado no
																									// cadastro do
																									// Gov.br. Poder�
																									// ter o valor
																									// "true" ou
																									// "false")
		System.out.println("E-Mail: " + idTokenJwtClaims.getClaimValue("email")); // (Endere�o de e-mail cadastrado no
																					// Gov.br do usu�rio autenticado.
																					// Caso o atributo email_verified do
																					// ID_TOKEN tiver o valor false, o
																					// atributo email n�o vir� no
																					// ID_TOKEN)
		System.out.println("Telefone Validado: " + idTokenJwtClaims.getClaimValue("phone_number_verified")); // (Confirma
																												// se o
																												// telefone
																												// foi
																												// validado
																												// no
																												// cadastro
																												// do
																												// Gov.br.
																												// Poder�
																												// ter o
																												// valor
																												// "true"
																												// ou
																												// "false")
		System.out.println("Telefone: " + idTokenJwtClaims.getClaimValue("phone_number")); // (N�mero de telefone
																							// cadastrado no Gov.br do
																							// usu�rio autenticado. Caso
																							// o atributo
																							// phone_number_verified do
																							// ID_TOKEN tiver o valor
																							// false, o atributo
																							// phone_number n�o vir� no
																							// ID_TOKEN)
		System.out.println("Link para a foto: " + fotoUrl); // URL de acesso � foto do usu�rio cadastrada no Gov.br. A
															// mesma � protegida e pode ser acessada passando o access
															// token recebido.
		System.out.println("CNPJ: " + idTokenJwtClaims.getClaimValue("cnpj")); // CNPJ vinculado ao usu�rio autenticado.
																				// Atributo ser� preenchido quando
																				// autentica��o ocorrer por certificado
																				// digital de pessoal jur�dica.
		System.out.println("Nome Empresa CNPJ " + idTokenJwtClaims.getClaimValue("cnpj_certificate_name")); // Nome da
																											// empresa
																											// vinculada
																											// ao
																											// usu�rio
																											// autenticado.
																											// Atributo
																											// ser�
																											// preenchido
																											// quando
																											// autentica��o
																											// ocorrer
																											// por
																											// certificado
																											// digital
																											// de
																											// pessoal
																											// jur�dica.

		List<String> listaAMR = accessTokenJwtClaims.getStringListClaimValue("amr");

		System.out.println("\n\nDados da Autentica��o:");
		System.out.println("Amr: " + String.join(",", listaAMR)); // Lista com os fatores de autentica��o do usu�rio.
																	// Pode ser �passwd� se o mesmo logou fornecendo a
																	// senha, ou �x509� se o mesmo utilizou certificado
																	// digital ou certificado em nuvem.

		/**
		 * Servi�o 1: De posse do access token, a aplica��o pode chamar o servi�o para
		 * receber a foto do usu�rio.
		 */

		String resultadoFoto = getFoto(fotoUrl, accessToken);

		System.out.println("\n--------------------Servi�o 1 - Foto do usu�rio------------------");
		System.out.println("Foto retornada:");
		System.out.println(resultadoFoto);

		/**
		 * Servi�o 2: De posse do access token, a aplica��o pode chamar o servi�o para
		 * saber quais n�veis o usu�rio logado possui.
		 */

		String niveisJson = getNiveis(accessToken, idTokenJwtClaims.getSubject());

		System.out.println(
				"\n--------------------Servi�o 2 - Informa��es acerca dos n�veis do usu�rio------------------");
		System.out.println("JSON retornado:");
		System.out.println(niveisJson);

		/**
		 * Servi�o 3: De posse do access token, a aplica��o pode chamar o servi�o para
		 * saber quais categorias o usu�rio logado possui.
		 */

		String categoriasJson = getCategorias(accessToken, idTokenJwtClaims.getSubject());

		System.out.println(
				"\n--------------------Servi�o 2 - Informa��es acerca das categorias do usu�rio------------------");
		System.out.println("JSON retornado:");
		System.out.println(categoriasJson);

		/**
		 * Servi�o 4: De posse do access token, a aplica��o pode chamar o servi�o para
		 * saber quais selos o usu�rio logado possui.
		 */

		String confiabilidadeJson = getConfiabilidade(accessToken, idTokenJwtClaims.getSubject());

		System.out.println(
				"\n--------------------Servi�o 2 - Informa��es acerca da confiabilidade do usu�rio------------------");
		System.out.println("JSON retornado:");
		System.out.println(confiabilidadeJson);

		/**
		 * Servi�o 5: De posse do access token, a aplica��o pode chamar o servi�o para
		 * saber quais empresas se encontram vinculadas ao usu�rio logado.
		 *
		 */

		String empresasJson = getEmpresasVinculadas(accessToken, idTokenJwtClaims.getSubject());

		System.out.println("\n--------------------Servi�o 3 - Empresas vinculadas ao usu�rio------------------");
		System.out.println("JSON retornado:");
		System.out.println(empresasJson);

		/**
		 * Servi�o 6: De posse do access token, a aplica��o pode chamar o servi�o para
		 * obter dados de uma empresa espec�fica e o papel do usu�rio logado nesta
		 * empresa.
		 */


		JSONArray empresasVinculadasJson = (JSONArray) parser.parse(empresasJson);
			
		if (!empresasVinculadasJson.isEmpty()) {

			for (Object cnpjSeparado : empresasVinculadasJson) {
				
				JSONObject cnpj = (JSONObject) cnpjSeparado;
				
				String dadosEmpresaJson = getDadosEmpresa(accessToken, cnpj.get("cnpj").toString(),
						idTokenJwtClaims.getSubject());

				System.out.printf("\n--------------------Servi�o 4 - Informa��es acerca da empresa %s------------------",
						cnpj.get("cnpj").toString());
				System.out.println("JSON retornado:");
				System.out.println(dadosEmpresaJson);
				
			}
		}
		
		System.out.println("--------------------Catalogo de Confiabildiades (Selos)------------------");
		System.out.println("Abra um Browser (Chrome ou Firefox), aperte F12. Clique na aba 'Network'.");
		System.out.println(
				"Cole a URL abaixo no Browser (Chrome ou Firefox) para verificar apresenta��o do cat�logo de confiabilidades (selos).");
		System.out.println(URL_CATALOGO_SELOS + "/?client_id=" + CLIENT_ID + "&niveis=" + NIVEIS + "&categorias="
				+ CATEGORIAS + "&confiabilidades=" + CONFIABILIDADES);

	}

	private static String extractToken(String code) throws Exception {
		String retorno = "";

		String redirectURIEncodedURL = URLEncoder.encode(REDIRECT_URI, "UTF-8");

		URL url = new URL(URL_PROVIDER + "/token?grant_type=authorization_code&code=" + code + "&redirect_uri="
				+ redirectURIEncodedURL);
		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Accept", "application/json");
		conn.setRequestProperty("authorization", String.format("Basic %s",
				Base64.getEncoder().encodeToString(String.format("%s:%s", CLIENT_ID, SECRET).getBytes())));

		if (conn.getResponseCode() != 200) {
			throw new RuntimeException("Falhou : HTTP error code : " + conn.getResponseCode());
		}

		BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

		String tokens = null;
		while ((tokens = br.readLine()) != null) {
			retorno += tokens;
		}

		conn.disconnect();

		return retorno;
	}

	private static JwtClaims processToClaims(String token) throws Exception {
		URL url = new URL(URL_PROVIDER + "/jwk");
		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Accept", "application/json");
		if (conn.getResponseCode() != 200) {
			throw new RuntimeException("Falhou : HTTP error code : " + conn.getResponseCode());
		}

		BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

		String ln = null, jwk = "";
		while ((ln = br.readLine()) != null) {
			jwk += ln;
		}

		conn.disconnect();

		JSONParser parser = new JSONParser();
		JSONObject tokensJson = (JSONObject) parser.parse(jwk);

		JSONArray keys = (JSONArray) tokensJson.get("keys");

		JSONObject keyJSONObject = (JSONObject) keys.get(0);

		String key = keyJSONObject.toJSONString();

		PublicJsonWebKey pjwk = PublicJsonWebKey.Factory.newPublicJwk(key);

		JwtConsumer jwtConsumer = new JwtConsumerBuilder().setRequireExpirationTime() // Exige que o token tenha um
																						// tempo de validade
				.setExpectedAudience(CLIENT_ID).setMaxFutureValidityInMinutes(60) // Testa se o tempo de validade do
																					// access token � inferior ou igual
																					// ao tempo m�ximo estipulado (Tempo
																					// padr�o de 60 minutos)
				.setAllowedClockSkewInSeconds(30) // Esta � uma boa pr�tica.
				.setRequireSubject() // Exige que o token tenha um Subject.
				.setExpectedIssuer(URL_PROVIDER + "/") // Verifica a proced�ncia do token.
				.setVerificationKey(pjwk.getPublicKey()) // Verifica a assinatura com a public key fornecida.
				.build(); // Cria a inst�ncia JwtConsumer.

		return jwtConsumer.processToClaims(token);
	}

	private static String getEmpresasVinculadas(String accessToken, String cpf) throws Exception {
		String retorno = "";

		URL url = new URL(URL_SERVICOS + "/empresas/v2/empresas?filtrar-por-participante=" + cpf);
		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Accept", "application/json");
		conn.setRequestProperty("authorization", "Bearer " + accessToken);

		if (conn.getResponseCode() != 200) {
			throw new RuntimeException("Falhou : HTTP error code : " + conn.getResponseCode());
		}

		String output;
		BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

		while ((output = br.readLine()) != null) {
			retorno += output;
		}

		conn.disconnect();

		return retorno;
	}

	private static String getDadosEmpresa(String accessToken, String cnpj, String cpf) throws Exception {
		String retorno = "";

		URL url = new URL(URL_SERVICOS + "/empresas/v2/empresas/" + cnpj + "/participantes/" + cpf);
		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Accept", "application/json");
		conn.setRequestProperty("authorization", "Bearer " + accessToken);

		if (conn.getResponseCode() != 200) {
			throw new RuntimeException("Falhou : HTTP error code : " + conn.getResponseCode());
		}

		String output;
		BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

		while ((output = br.readLine()) != null) {
			retorno += output;
		}

		conn.disconnect();

		return retorno;
	}

	private static String getFoto(String fotoUrl, String accessToken) throws Exception {
		URL url = new URL(fotoUrl);
		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Accept", "application/json");
		conn.setRequestProperty("Authorization", "Bearer " + accessToken);

		if (conn.getResponseCode() != 200) {
			return "Foto nao encontrada: " + conn.getResponseCode();
		}

		String foto = null;
		try (InputStream inputStream = conn.getInputStream();
				ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			IOUtils.copy(inputStream, baos);
			String mimeType = conn.getHeaderField("Content-Type");
			foto = new String("data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(baos.toByteArray()));
		}

		conn.disconnect();

		return foto;
	}

	private static String getNiveis(String accessToken, String cpf) throws Exception {
		String retorno = "";

		URL url = new URL(URL_SERVICOS + "/confiabilidades/v3/contas/" + cpf + "/niveis?response-type=ids");
		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Accept", "application/json");
		conn.setRequestProperty("Authorization", "Bearer " + accessToken);

		if (conn.getResponseCode() != 200) {
			throw new RuntimeException("Falhou : HTTP error code : " + conn.getResponseCode());
		}

		String output;
		BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

		while ((output = br.readLine()) != null) {
			retorno += output;
		}

		conn.disconnect();

		return retorno;
	}

	private static String getCategorias(String accessToken, String cpf) throws Exception {
		String retorno = "";

		URL url = new URL(URL_SERVICOS + "/confiabilidades/v3/contas/" + cpf + "/categorias?response-type=ids");
		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Accept", "application/json");
		conn.setRequestProperty("Authorization", "Bearer " + accessToken);

		if (conn.getResponseCode() != 200) {
			throw new RuntimeException("Falhou : HTTP error code : " + conn.getResponseCode());
		}

		String output;
		BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

		while ((output = br.readLine()) != null) {
			retorno += output;
		}

		conn.disconnect();

		return retorno;
	}

	private static String getConfiabilidade(String accessToken, String cpf) throws Exception {
		String retorno = "";

		URL url = new URL(URL_SERVICOS + "/confiabilidades/v3/contas/" + cpf + "/confiabilidades?response-type=ids");
		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Accept", "application/json");
		conn.setRequestProperty("Authorization", "Bearer " + accessToken);

		if (conn.getResponseCode() != 200) {
			throw new RuntimeException("Falhou : HTTP error code : " + conn.getResponseCode());
		}

		String output;
		BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

		while ((output = br.readLine()) != null) {
			retorno += output;
		}

		conn.disconnect();

		return retorno;
	}

	private static String createRandomNumber() {
		return new BigInteger(50, new SecureRandom()).toString(16);

	}

}
