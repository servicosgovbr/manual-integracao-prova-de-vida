﻿Passo a passo
================================

Solicitação de acesso 
+++++++++++++++++++++++++++

Para informações sobre processo de formalização da integração e solicitação de credenciais da API de assinatura (testes e produção), o Gestor do serviço público deve acessar a página `Serviço de integração aos Produtos de Identidade Digital gov.br`_. Caso já tenha formalizado o processo de integração, o contato para suporte técnico é: int-assinatura-govbr@economia.gov.br.


.. note::
	Para consumir os serviços da API de assinatura, há **obrigatoriedade**  que a aplicação do órgão esteja previamente 
	integrada à Plataforma de Autenticação Digital do Cidadão -  `Login Único`_. Ainda assim, a autorização de acesso utilizada pela assinatura 
	é condicionada ao processo de autorização explícita do usuário, conforme `Lei n° 14.063`_ Art.4º. O usuário deve conceder a autorização para Assinatura 
	API Service assinar digitalmente um documento em nome deste usuário e essa autorização é solicitada durante o fluxo de autorização OAuth da API de assinatura. 
	Por esse motivo que a liberação de acesso para emissão do certificado e a permissão da assinatura implica a geração de uma requisição ao servidor OAuth que controla os recursos desta API. 
   

Orientações para testes  
++++++++++++++++++++++++++

De Acordo com a portaria `SEDGGME Nº 2.154/2021`_ as identidades digitais da plataforma gov.br são classificadas em três tipos: Bronze, Prata e Ouro. A identidade bronze permite ao usuário somente a realização de assinaturas simples. Nesta plataforma para realizar uma assinatura avançada, seja qual for o ambiente, o usuário deve possuir identidade digital prata ou ouro. Caso o usuário não possua este nível de identidade, a aplicação cliente deverá emitir mensagem informando ao usuário. Segue um exemplo de mensagem:                             
"É necessário possuir conta gov.br nível ouro ou prata para utilizar a assinatura eletrônica digital. Clique aqui para aumentar o nível da sua conta." A aplicação cliente deve direcionar o usuário para o serviço de Catálogo de Confiabilidades. Os parâmetros para requisição deste serviço estão descritos no roteiro de integração do Login Único no link https://manual-roteiro-integracao-login-unico.servicos.gov.br/pt/stable/iniciarintegracao.html#acesso-ao-servico-de-catalogo-de-confiabilidades-selos

.. important::
   Documentos assinados digitalmente no ambiente de **HOMOLOGAÇÃO** são validados em: https://verificador.staging.iti.br 
   Documentos assinados no ambiente de **PRODUÇÃO** podem ser validados no serviço de validação de assinaturas eletrônicas do ITI https://validar.iti.gov.br

Criar uma conta nível prata gov.br  
+++++++++++++++++++++++++++++++++++++++

1. Acesse https://sso.staging.acesso.gov.br/ e siga passos do tutorial abaixo:

`Tutorial conta prata <https://github.com/servicosgovbr/manual-integracao-assinatura-eletronica/raw/main/arquivos/Tutorial.pdf>`_

API de assinatura digital gov.br
+++++++++++++++++++++++++++++++++++++

Esta API de assinatura segue os princípios do estilo de arquitetura REST e fornece serviços web baseados em HTTP que implementam a assinatura digital utilizando certificados avançados gov.br. 
Para acesso a esses serviços a API adota o uso do protocolo OAuth 2.0, que é um padrão aberto de delegação de autorização. Deste modo, o uso da API envolve duas etapas:

1. Geração do token de acesso (Access Token)

2. Consumo dos serviços de assinatura da API

Geração do access token
+++++++++++++++++++++++

**Passo 1: Gerar code**

**Endereço servidor autorização:** 

.. code-block:: console

	https://cas.staging.iti.br/oauth2.0/authorize?

A aplicação cliente deve redirecionar o navegador do usuário para o endereço do servidor de autorização para de obter seu consentimento para o uso de seu certificado para a assinatura. Nesse processo, a aplicação deve usar credenciais previamente autorizadas no servidor. Esta requisição possui os parâmetros abaixo:

==================  ==================================================================================================
**Parâmetro**  	    **Valor**
------------------  --------------------------------------------------------------------------------------------------
**response_type**	code
**client_id**       Chave de acesso, que identifica o serviço consumidor da aplicação cadastrada.
**scope**           sign ou signature_session
**redirect_uri**    URL de retorno cadastrada para a aplicação cliente. Não necessita utilizar o formato URL Encode.
**state**           Valor usado para manter o estado entre a solicitação e o retorno de chamada.
**nonce**           Sequência de caracteres usado para associar uma sessão do serviço consumidor ao token.
==================  ==================================================================================================

.. important::
  Deve-se utilizar o parâmetro **scope** com valor **sign** para gerar um token que permite a assinatura de um único hash. Este token gerado só pode ser utilizado uma única vez. Na tentativa de uma nova assinatura com esse mesmo token, um erro será retornado. Para gerar um token que permita a assinatura de mais de um hash (assinatura em lote), deve ser utilizado o valor **signature_session**. Neste caso, durante a validade do token, este poderá ser utilizado para realizar várias assinaturas.

.. code-block:: console

    https://<Servidor OAuth>/authorize?response_type=code&redirect_uri=<URI de redirecionamento>&scope=sign&client_id=<client_id

Neste endereço, o serviço pede a autorização expressa do usuário para acessar seu certificado para assinatura. Neste instante será pedido um código de autorização a ser enviado por SMS.

.. Attention::
  No ambiente de homologação, o código de autorização é enviado por SMS e também pode ser utilizado o código **12345**. No ambiente de **Produção** o SMS é enviado por notificação do aplicativo gov.br ou por SMS se usuário não possuir aplicativo gov.br instalado.
  

Após a autorização, o usuário é redirecionado para o endereço <URI de redirecionamento> enviado no **redirect_uri** e retorna, como um parâmetro de query, o atributo **code** e o atributo **state**. O <URI de redirecionamento> deve ser um endpoint da aplicação correspondente ao padrão autorizado no servidor de autorização, e capaz de receber e tratar o parâmetro “code”. Este atributo deve ser utilizado na fase seguinte para solicitar um Access Token ao servidor de autorização. 

.. note::
	A URL de retorno deve pertencer ao domínio do órgão. Por exemplo: https://www.nomeorgao.gov.br/assinar. Cada órgão e ou serviço que será integrado a API de assinatura deve solicitar credenciais separadas.


**Passo 2: Solicitar Access Token**

Realizar a seguinte requisição HTTP com método POST para o endereço https://cas.staging.iti.br/oauth2.0/token? passando as informações abaixo:

==================  ==================================================================================================
**Parâmetro**  	    **Valor**
------------------  --------------------------------------------------------------------------------------------------
**code**            Código de autorização gerado pelo servidor.
**client_id**       Chave de acesso, que identifica o serviço consumidor da aplicação cadastrada.
**grant_type**      authorization_code
**client_secret**   Chave secreta conhecida apenas pela aplicação cliente e servidor de autorização.
**redirect_uri**    URI de retorno cadastrada para a aplicação cliente. 
==================  ==================================================================================================

O parâmetro <redirect_uri> deve ter exatamente o mesmo valor informado no passo 1. Sendo feita corretamente as duas requisições, o servidor OAuth retornará um objeto JSON contendo o Access Token, que deve ser usado nas requisições subsequentes aos endpoints do serviço.

**Exemplo de código HTTP de sucesso:**

Retorno **200**: sucesso

.. code-block:: JSON

	{
    	"access_token": "AT-104-1zJeYZGuEUHkF4ovJGONi0U-jB342xH0",
    	"token_type": "bearer",
    	"expires_in": 360
	}

**Exemplos de códigos HTTP de erro:**

Retorno **401**: Algum valor do parâmetro informado incorretamente. Exemplo:

.. code-block:: JSON

	{ 
		"timestamp": 1688566398186,
		"status": 401,
		"error": "Unauthorized",
		"message": "No message available",
		"path": "/oauth2.0/token"
	} 

Retorno **400**: Parâmetro <code> utilizado por mais de uma vez ou inválido.

.. code-block:: console

	error=invalid_request


.. note::
  O servidor OAuth de homologação está delegando a autenticação ao ambiente de **staging** do gov.br.


**Importante**: Para valor do parâmetro **scope** igual a **sign**, o access token gerado autoriza o uso da chave privada do usuário para a confecção de uma **única** assinatura eletrônica avançada. O token deve ser usado em até 10 minutos. O tempo de validade do token poderá ser modificado no futuro à discrição do ITI. No caso do valor do parâmetro **scope** igual a **signature_session** (assinatura em lote), o access token gerado autoriza o uso da chave privada do usuário para a confecção de **várias** assinaturas eletrônicas avançadas durante o prazo de validade do token.

Obtenção do certificado do usuário
++++++++++++++++++++++++++++++++++

Para obtenção do certificado do usuário deve-se fazer uma requisição HTTP GET para endereço https://assinatura-api.staging.iti.br/externo/v2/certificadoPublico enviando o cabeçalho Authorization com o tipo de autorização Bearer e o access token obtido anteriormente. Segue abaixo o parâmetro do Header para requisição:

==================  ======================================================================
**Parâmetro**  	**Valor**
------------------  ----------------------------------------------------------------------
**Authorization**   Bearer <access token>
==================  ======================================================================

Exemplo de requisição:

.. code-block:: console

		GET /externo/v2/certificadoPublico HTTP/1.1
		Host: assinatura-api.staging.iti.br 
		Authorization: Bearer AT-183-eRE7ot2y3FpEOTCIo1gwnZ81LMmT5I8c

Será retornado o certificado digital com formato PEM na resposta.

.. Attention::
	Para emissão do certificado é realizada, previamente, a validação da situação cadastral do CPF e do nível identidade da conta gov.br do usuário.

**Nível de identidade bronze**
Se usuário possui nível identidade bronze a API impede a emissão de certificado e retorna código e mensagem abaixo:

Response: **403**

.. code-block:: console

		Cidadão não possui a identidade (Prata ou Ouro) necessária para uso da assinatura eletrônica digital.

**CPF situação cancelada, nula, falecido**
Se CPF de usuário com as seguintes situações:
1. Titular Falecido - quando há data de óbito vinculada ao CPF;
2. Cancelada por Multiplicidade - quando há mais de uma inscrição no CPF para a mesma pessoa; nesse caso, elege-se um para permanecer ativo e os demais são vinculados a ele;
3. Nula - quando constatada a fraude.
4. Cancelada de Ofício - ato de ofício, no interesse da administração tributária ou determinação judicial.
A API impede a emissão de certificado e retorna código e mensagem abaixo:

Response: **403**

.. code-block:: console

		CPF com situação cancelada, nula ou falecido na Receita Federal não permite uso da assinatura eletrônica digital.


Realização da assinatura digital de um HASH SHA-256 em PKCS#7
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

Para gerar um pacote PKCS#7 contendo a assinatura digital de um HASH SHA-256 utilizando a chave privada do usuário, deve-se fazer uma requisição HTTP POST para o endereço https://assinatura-api.staging.iti.br/externo/v2/assinarPKCS7 enviando os seguintes parâmetros:

==================  ======================================================================
**Parâmetros**  	**Valor**
------------------  ----------------------------------------------------------------------
**Content-Type**    application/json       
**Authorization**   Bearer <access token>
==================  ======================================================================

Body da requisição:

.. code-block:: JSON

	{ "hashBase64": "<Hash SHA256 codificado em Base64>"} 

Exemplo de requisição:

.. code-block:: console

		POST /externo/v2/assinarPKCS7 HTTP/1.1
		Host: assinatura-api.staging.iti.br 
		Content-Type: application/json	
		Authorization: Bearer AT-183-eRE7ot2y3FpEOTCIo1gwnZ81LMmT5I8c

		{"hashBase64":"kmm8XNQNIzSHTKAC2W0G2fFbxGy24kniLuUAZjZbFb0="}

Será retornado um arquivo contendo o pacote PKCS#7 com a assinatura digital do hash SHA256-RSA e com o certificado público do usuário. O arquivo retornado pode ser validado em https://verificador.staging.iti.br/.

.. Attention::
	Do mesmo modo do serviço para obtenção do certificado, para gerar uma ou mais assinaturas é realizada, previamente, a validação da situação cadastral do CPF e do nível identidade da conta gov.br do usuário.

**Nível de identidade bronze**
Se usuário possui nível identidade bronze a API impede a assinatura e retorna código e mensagem abaixo:

Response: **403**

.. code-block:: console

		Cidadão não possui a identidade (Prata ou Ouro) necessária para uso da assinatura eletrônica digital.

**CPF situação cancelada, nula, falecido**
Se CPF de usuário com as seguintes situações:
1. Titular Falecido - quando há data de óbito vinculada ao CPF;
2. Cancelada por Multiplicidade - quando há mais de uma inscrição no CPF para a mesma pessoa; nesse caso, elege-se um para permanecer ativo e os demais são vinculados a ele;
3. Nula - quando constatada a fraude.
4. Cancelada de Ofício - ato de ofício, no interesse da administração tributária ou determinação judicial.
A API impede a assinatura e retorna código e mensagem abaixo:

Response: **403**

.. code-block:: console

		CPF com situação cancelada, nula ou falecido na Receita Federal não permite uso da assinatura eletrônica digital.

**Assinatura em Lote**: Para gerar múltiplos pacotes PKCS#7, cada qual correspondente a assinatura digital de um HASH SHA-256 distinto (correspondentes a diferentes documentos), deve-se seguir as orientações do tópico **Geração do Access Token** para solicitação do token que permita esta operação (scope signature_session). Após a obtenção deste token, deve ser feita uma requisição para o endereço https://assinatura-api.staging.iti.br/externo/v2/assinarPKCS7 para cada hash a ser assinado, enviando os mesmo parâmetros informados acima. No código de **Exemplo de aplicação** pode-se verificar no arquivo assinar.php um exemplo de implementação da chamada ao serviço para uma assinatura em lote. O retorno desta operação será um arquivo contendo o pacote PKCS#7 correspondente a cada hash enviado na requisição ao serviço.

Assinaturas PKCS#7 e PDF
+++++++++++++++++++++++++

Existem duas formas principais de assinar um documento PDF:

* Assinatura *detached*
* Assinatura envelopada

A Assinatura *detached* faz uso de dois arquivos: (1) o arquivo PDF a ser assinado; e (2) um arquivo de assinatura (**.p7s**). Nesta modalidade de assinatura, nenhuma informação referente à assinatura é inclusa no PDF. Toda a informação da assinatura está encapsulada no arquivo (.p7s).
Qualquer alteração no PDF irá invalidar a assinatura contida no arquivo no arquivo (.p7s). Para validar esta modalidade de assinatura, é necessário apresentar para o software de verificação os dois arquivos, PDF e (.p7s).

Para realizar esta modalidade de assinatura pela API de assinatura eletrônica avançada, deve-se calcular o hash sha256 sobre todo o arquivo PDF e enviá-lo através da operação **assinarPKCS7** detalhada no tópico anterior. O arquivo binário retornado como resposta desta operação deve ser salvo com a extensão (.p7s).

A assinatura envelopada, por sua vez, inclui dentro do próprio arquivo PDF o pacote de assinatura PKCS#7. Portanto, não há um arquivo de assinatura separado. Para realizar essa modalidade de assinatura deve-se:

1. Preparar o documento de assinatura
2. Calcular quais os *bytes (bytes-ranges)* do arquivo preparado no passo 1 deverão entrar no computo do hash. Diferentemente da assinatura *detached*, o cálculo do hash para assinatura envelopadas em PDF não é o hash SHA256 do documento original (integral). É uma parte do documento preparado no passo 1.
3. Calcular o hash SHA256 desses *bytes* 
4. Submeter o hash SHA256 à operação **assinarPKCS7** desta API.
5. O resultado da operação **assinarPKCS7** deve ser codificado em hexadecimal e embutido no espaço que foi previamente alocado no documento no passo 1.

O detalhamento de como preparar o documento, calcular os *bytes-ranges* utilizados no computo do hash e como embutir o arquivo PKCS7 no arquivo previamente preparado podem ser encontrados na especificação ISO 32000-1:2008. Existem bibliotecas que automatizam esse procedimento de acordo com o padrão (ex: PDFBox para Java e iText para C#).

Recomendações para assinaturas digitais em PDF
++++++++++++++++++++++++++++++++++++++++++++++

O PDF foi especificado e desenvolvido pela empresa Adobe System. A partir da versão PDF 1.6, a Adobe utiliza o padrão ISO 32000-1 em sua especificação. Este padrão define a especificação do formato digital para representação de um documento PDF de forma que permita aos usuários trocar e visualizar documentos independente do ambiente que eles foram criados. Resumidamente, a especificação define a estrutura do conteúdo do arquivo PDF, como este conteúdo pode ser interpretado, acessado, atualizado e armazenado dentro do arquivo.

O padrão PDF possui a funcionalidade chamada **Atualização Incremental**. Essa funcionalidade permite que o PDF seja modificado acrescentando novas informações após o fim do arquivo. A assinatura de PDF é realizada incorporando uma assinatura digital ao fim do PDF utilizando o mecanismo de Atualização Incremental. Este tipo de implementação protege contra modificação todas as informações anteriores a Assinatura Digital a ser realizada e a própria Assinatura Digital incluída no arquivo. Entretanto, ela não impede que novas Atualizações Incrementais sejam realizadas, alterando visualmente o PDF após uma assinatura ter sido incluída. Ainda assim, sempre é possível recuperar a versão que foi efetivamente assinada, e esta versão não pode ser modificada de forma alguma.

A possibilidade de alteração visual em documentos previamente assinados pode causar confusão por parte de cidadãos e órgãos públicos no momento da validação e verificação de documentos assinados. Por esta razão a partir da Versão 1.5 do PDF, foi introduzido um mecanismo para proteção e controle de alterações passíveis de serem realizadas em documentos PDF assinados. Esse mecanismo é chamado **MDP (modification detection and prevention - DocMDP)**, e permite que a primeira pessoa a assinar o documento, ou seja, o autor, possa especificar quais alterações poderão ser realizadas em futuras atualizações incrementais.

Recomenda-se fortemente que a **primeira assinatura realizada** em um documento PDF seja configurada da seguinte forma:

1. Incluir entrada *Reference*, com uma referência indireta a um Dicionário *“Signature Reference”*. Exemplo:

.. code-block:: console

		166 0 obj
		<<
		/Type /Sig
		/Filter /Adobe.PPKLite
		/SubFilter /adbe.pkcs7.detached
		/M (D:20220705145549-03'00')
		/Reference [168 0 R]
		/Contents <24730....>
		/ByteRange [0 36705 55651 8985] 
		>>
		Endobj
		
2. O dicionário *“Signature Reference”* conter as entradas *“Transform Method”* com o valor DocMDP; e, *“TransformParams”* com uma referência indireta para um dicionário de *TransformParams*. Exemplo:

.. code-block:: console

		168 0 obj
		<<
		/Type /SigRef
		/TransformMethod /DocMDP
		/TransformParams 170 0 R
		>>
		
3. O dicionário *“TransformParams”* com uma entrada *P* com valor 2 e entrada *V* com valor 1.2.

.. code-block:: console

		170 0 obj
		<<
		/Type /TransformParams
		/P 2
		/V /1.2
		>>

.. important::
	 Não é recomendado o uso do dicionário */Perms* com entrada */DocMDP* por questões de compatibilidade com o Adobe. 
	 Ao configurar a primeira assinatura desta forma apenas serão permitidas as seguintes alterações: **Preenchimento de formulários, templates e inclusão de novas assinaturas**.

Outros valores de *P* possíveis de serem usados: 

* **P = 1** -> Nenhuma alteração é admitida; 
* **P = 2** -> Alterações permitidas em formulários, templates e inclusão de novas assinaturas; e
* **P = 3** -> Além das permissões admitidas para P = 2, admite-se também anotações, deleções e modificações.

.. note::
	A utilização da logo gov.br é permitida nas assinaturas que adicionam imagem ao PDF. A orientações quanto a aplicação da logo podem ser verificadas 
	em Manual de uso da marca `Link manual`_

Exemplo de aplicação
++++++++++++++++++++

Logo abaixo, encontra-se um pequeno exemplo PHP para prova de conceito.

`Download Exemplo PHP <https://github.com/servicosgovbr/manual-integracao-assinatura-eletronica/raw/main/downloadFiles/exemploApiPhp.zip>`_

Este exemplo é composto por 3 arquivos:

1. index.php -  Formulário para upload de um arquivo
2. upload.php - Script para recepção de arquivo e cálculo de seu hash SHA256. O Resultado do SHA256 é armazenado na sessão do usuário.
3. assinar.php - Implementação do handshake OAuth, assim como a utilização dos dois endpoints acima. Como resultado, uma página conforme a figura abaixo será apresentada, mostrando o certificado emitido para o usuário autenticado e a assinatura.


.. image:: images/image.png


Para executar o exemplo, é possível utilizar Docker com o comando abaixo:

.. code-block:: console
	
		docker-compose up -d

e acessar o endereço http://127.0.0.1:8080

Como criar um par de chaves PGP
+++++++++++++++++++++++++++++++

**GnuPG para Windows** 

Faça o download do aplicativo Gpg4win em: https://gpg4win.org/download.html
O Gpg4win é um pacote de instalação para qualquer versão do Windows, que inclui o software de criptografia GnuPG. Siga abaixo as instruções detalhadas de como gerar um par de chaves PGP:

1. Após o download, execute a instalação e deixe os seguintes componentes marcados conforme imagem abaixo:

.. image:: images/pgp1.png

2. Concluída a instalação, execute o **Kleopatra** para a criação do par de chaves. Kleopatra é uma ferramenta para gerenciamento de certificados X.509, chaves PGP e também para gerenciamento de certificados de servidores. A janela principal deverá se parecer com a seguinte:

.. image:: images/pgp2.png

3. Para criar novo par de chaves (pública e privada), vá até o item do Menu **Arquivo** → **Novo Par de chaves...** selecione **Criar um par de chaves OpenPGP pessoal**. Na tela seguinte informe os detalhes **Nome** e **Email**, marque a opção para proteger a chave com senha e clique em **Configurações avançadas...**

4. Escolha as opções para o tipo do par de chaves e defina uma data de validade. Esta data pode ser alterada depois. Após confirmação da tela abaixo, abrirá uma janela para informar a senha. O ideal é colocar uma senha forte, que deve conter pelo menos 8 caracteres, 1 digito ou caractere especial.

.. image:: images/pgp3.png

5. Após concluído, o sistema permite o envio da chave pública por email clicando em **Enviar chave pública por e-mail...** ou o usuário tem a opção de clicar em **Terminar** e exportar a chave pública para enviá-la por email posteriormente. Para exportar a chave pública e enviá-la anexo ao email, clique com
botão direito na chave criada e depois clique em **Exportar...**

**GnuPG para Linux** 

Praticamente todas as distribuições do Linux trazem o GnuPG instalado e para criar um par de chaves pública e privada em nome do utilizador 'Fulano de Tal', por exemplo, siga os passos abaixo:


1. Abra o terminal e execute o comando abaixo e informe os dados requisitados (Nome e Email). Se não forem especificados os parâmetros adicionais, o tipo da chave será RSA 3072 bits. Será perguntado uma frase para a senha (frase secreta, memorize-a), basta responder de acordo com o que será pedido.

.. code-block:: console

		$ gpg --gen-key
		
		gpg (GnuPG) 2.2.19; Copyright (C) 2019 Free Software Foundation, Inc.
		This is free software: you are free to change and redistribute it.
		There is NO WARRANTY, to the extent permitted by law.
		gpg: directory '/home/user/.gnupg' created
		gpg: keybox '/home/user/.gnupg/pubring.kbx' created
		Note: Use "gpg --full-generate-key" for a full featured key generation dialog.

	    O GnuPG precisa construir uma ID de usuário para identificar sua chave.

		Nome completo: **Fulano de Tal**
		Endereço de correio eletrônico: **fulanodetal@email.com**
		Você selecionou este identificador de usuário: "Fulano de Tal <fulanodetal@email.com>"
		Change (N)ame, (E)mail, or (O)kay/(Q)uit? O

		gpg: /home/user/.gnupg/trustdb.gpg: banco de dados de confiabilidade criado
        gpg: chave D5882F501CC722AA marcada como plenamente confiável
        gpg: directory '/home/user/.gnupg/openpgp-revocs.d' created
        gpg: revocation certificate stored as '/home/user/.gnupg/openpgprevocs.d/269C3D6B65B150A9B349170D5882F501CC722AA.rev'

		Chaves pública e privada criadas e assinadas.

		pub rsa3072 2021-04-30 [SC] [expira: 2023-04-30] 269C3D6B65B150A9B349170D5882F501CC722AA uid Fulano de Tal <fulanodetal@email.com>
        sub rsa3072 2021-04-30 [E] [expira: 2023-04-30]
		
2. Para enviar um documento ou um e-mail cifrado com sua chave, é necessário que a pessoa tenha a sua chave pública. Partindo do ponto que a pessoa fez um pedido da sua chave pública, então é necessário criar um arquivo
com a chave e passar o arquivo para o solicitante (por exemplo, podemos passar pelo e-mail). Execute o comando abaixo no terminal do Linux para exportar a sua chave para o arquivo **MinhaChave.asc**

.. code-block:: console
	
		$ gpg --export 269C3D6B65B150A9B449170D5882F501CC722AA> MinhaChave.asc

A sequência de números e letras "269C3D6B65B150A9B349170D5882F501CC722AA" é o ID da chave (da chave que criamos aqui no exemplo, substitua pelo seu ID) e **MinhaChave.asc** é o nome do arquivo onde será gravada a chave (pode ser outro nome).
O próximo passo é o envio do arquivo com a chave pública para a pessoa e então ela poderá criptografar um e-mail ou um documento com a sua chave pública. Se foi criptografado com a sua chave pública, somente a sua chave privada será capaz de decodificar o documento e a frase secreta de sua chave será requisitada.

3. Para **decifrar** um documento que foi criptografado com a sua chave pública basta seguir os passos abaixo, substituindo **NomeArquivo.gpg** pelo nome do arquivo cifrado. Será solicitada a frase secreta de sua chave privada. Um arquivo com nome **ArquivoTextoClaro** será criado na mesma pasta. Este arquivo contêm as informações decifradas.		

.. code-block:: console
	
		$ gpg -d NomeArquivo.gpg > ArquivoTextoClaro

		gpg: criptografado com 3072-bit RSA chave, ID 4628820328759F85, criado 2021-04-24 "Fulano de Tal <fulanodetal@email.com>"






.. |site externo| image:: images/site-ext.gif
.. _`codificador para Base64`: https://www.base64decode.org/
.. _`solicitação integração`: https://www.gov.br/governodigital/pt-br/transformacao-digital/servico-de-integracao-aos-produtos-de-identidade-digital-gov.br
.. _`OpenID Connect`: https://openid.net/specs/openid-connect-core-1_0.html#TokenResponse
.. _`auth 2.0 Redirection Endpoint`: https://tools.ietf.org/html/rfc6749#section-3.1.2
.. _`Exemplos de Integração`: exemplointegracao.html
.. _`Design System do Governo Federal`: http://dsgov.estaleiro.serpro.gov.br/ds/componentes/button
.. _`Resultado Esperado do Acesso ao Serviço de Confiabilidade Cadastral (Selos)`: iniciarintegracao.html#resultado-esperado-do-acesso-ao-servico-de-confiabilidade-cadastral-selos
.. _`Resultado Esperado do Acesso ao Serviço de Confiabilidade Cadastral (Categorias)` : iniciarintegracao.html#resultado-esperado-do-acesso-ao-servico-de-confiabilidade-cadastral-categorias
.. _`Documento verificar Código de Compensação dos Bancos` : arquivos/TabelaBacen.pdf
.. _`Login Único`: https://manual-roteiro-integracao-login-unico.servicos.gov.br/pt/stable/index.html
.. _`Lei n° 14.063`: http://www.planalto.gov.br/ccivil_03/_ato2019-2022/2020/lei/L14063.htm
.. _`SEDGGME Nº 2.154/2021`: https://www.in.gov.br/web/dou/-/portaria-sedggme-n-2.154-de-23-de-fevereiro-de-2021-304916270
.. _`Link manual`: https://www.gov.br/ds/assets/govbr-ds-dev-core/downloads/GovbrManualMarca.pdf
