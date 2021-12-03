Iniciando a Integração
======================

Procedimentos para a Integração
+++++++++++++++++++++++++++++++

É possível provar que um cidadão está vivo **sem** usar as transações (API) da Prova de vida. Nesse caso, utiliza-se o serviço do Login Único, o qual verifica quais selos de confiabilidade a conta do cidadão possui. Para realizar a Prova de vida dessa forma, a aplicação cliente deve estar **obrigatoriamente** integrada ao sistema do `Login Único`_ |site externo|. 

Para provar a vida consumindo os serviços da API descrita nesta documentação, há necessidade de liberar o ambiente de **homologação** para que a aplicação cliente possa utilizar. Essa liberação ocorre por meio do envio das informações listadas abaixo:


* Credenciais de acesso aos serviços de Homologação

  - **CPF** de um representante do orgão ou entidade dona do serviço a ser integrado
  - **E-mail** do representante do orgão ou entidade 
  - **CPNJ** do orgão ou entidade

.. raw:: html
   
   <br>

* Para pessoas que realizarão testes no Aplicativo (Validação Facial), e **não** constam na base do TSE, deve-se enviar as informações abaixo para cadastro da biometria facial

  
  - **Nome**
  - **CPF**
  - **Foto** no formato 3X4

.. raw:: html
   
   <br>


Essas informações deverão ser encaminhadas para o e-mail: int-provavida-govbr@economia.gov.br, por um representante do órgão ou entidade. O representante ficará responsável pelas credenciais de acesso geradas por integrantes da Secretaria de Governança Digital (SGD) do Ministério da Economia (ME).


Métodos e interfaces de integração
+++++++++++++++++++++++++++++++++++

Autenticação
------------

Para que a autenticação aconteça, é preciso chamar serviço com intuito de adquirir um ticket de acesso (*token*) para os serviços protegidos da Prova de vida. O serviço de autenticação segue o padrão `OAuth 2.0`_ |site externo|.

A requisição é feita através de um POST para o endereço: https://h.meugov.estaleiro.serpro.gov.br/auth/oauth/token?grant_type=client_credentials

Parâmetros do Header para a requisição: 
https://h.meugov.estaleiro.serpro.gov.br/auth/oauth/token?grant_type=client_credentials

=================  ======================================================================
**Variável**  	   **Descrição**
-----------------  ----------------------------------------------------------------------
**Authorization**  Palavra **Basic** seguida da informação codificada em *Base64*, no seguinte formato: CLIENT_ID:CLIENT_SECRET (credenciais de acesso)(utilizar `codificador para Base64`_ |site externo|  para gerar codificação). 
=================  ======================================================================

Exemplo de *header*:

.. code-block:: console

	Authorization: Basic ZWM0MzE4ZDYtZjc5Ny00ZDY1LWI0ZjctMzlhMzNiZjRkNTQ0OkFJSDRoaXBfTUJYcVJkWEVQSVJkWkdBX2dRdjdWRWZqYlRFT2NWMHlFQll4aE1iYUJzS0xwSzRzdUVkSU5FcS1kNzlyYWpaZ3I0SGJuVUM2WlRXV1lJOA==

O serviço retornará, em caso de sucesso, no formato JSON, as informações conforme exemplo:

Response: **200**

.. code-block:: JSON

	{ 
		"access_token": "(Token de acesso a serviços protegidos da Prova de vida.)", 
		"token_type": "(O tipo do token gerado. Padrão: Bearer)", 
		"expires_in": "(Tempo de vida do token em segundos.)", 
		"scope": "(Escopos autorizados pelo provedor de autenticação. Padrão: '*')",
		"cnpj": "(CNPJ da organização solicitante da Prova de vida.)",
		"jti": "(Identificador único do token, reconhecido internamente pelo provedor de autenticação.)"
	} 


Transação da Prova de Vida
---------------------------

A Transação da Prova de Vida é com suporte a resposta automática utilizando selo biométrico GovBr. A resposta da requisição contém informações sobre validação facial feita pelo usuário em um momento anterior à solicitação. Portanto, a solicitação de Prova de vida pode ser **autorizada** automaticamente. 

A requisição possui o parâmetro "**selogovbr_reuso_em**" no *body*. O valor desse parâmetro é o intervalo de tempo em minutos anterior a data da transação. A Prova de vida será autorizada **automaticamente** caso o usuário tiver feito a validação facial dentro desse intervalo.

A Transação cria um pedido de Prova de vida para o cidadão (CPF). O Cidadão é informado via *push notification* no aplicativo "Gov.Br". 

Caso a Prova de vida **não** seja autorizada automaticamente, o usuário (cidadão) pode autorizar por confirmação ou por biometria facial no app "Gov.Br".

Parâmetros do Header para POST https://h.meugov.estaleiro.serpro.gov.br/api/vbeta3/transacoes

=================  ======================================================================
**Variável**       **Descrição**
-----------------  ----------------------------------------------------------------------
**Content-Type**   Tipo do conteúdo da requisição que está sendo enviada. Nesse caso estamos enviando como um *application/json*
**Authorization**  Palavra **Bearer** e o *access_token* da requisição POST do https://h.meugov.estaleiro.serpro.gov.br/auth/oauth/token?grant_type=client_credentials
=================  ======================================================================

Parâmetros do Body para POST https://h.meugov.estaleiro.serpro.gov.br/api/vbeta3/transacoes

.. code-block:: JSON

  { 
  "solicitante": {
          "cnpj": "(CNPJ do Solicitante.)",
          "nome": "(Orgão Solicitante.)",
          "servico": "(Nome do Serviço cliente.)"
         },
        "cpf": "(CPF do usuário que realizará a Prova de Vida.)",
        "motivo": "(Motivo da Prova de Vida. Exemplo: Obter benefício previdenciário)",
        "tipo": "(Tipo da solicitação. Padrão: 'B')",
        "selogovbr_reuso_em": "(Intervalo de tempo em minutos anterior a data da transação)",
        "expiracao_em": "(Tempo de vida da transação em minutos)",
        "mensagem_falha": "(Mensagem apresentada ao usuário no caso de falha na Prova de vida)",
        "mensagem_sucesso": "(Mensagem apresentada ao usuário no caso de sucesso na Prova de vida)",
        "categoria": "(Categoria da transação. Valor 'PV' para prova de vida ou valor 'OU' para outros tipos)"
  } 


Exemplo de *body*:

.. code-block:: JSON

  { 
  "solicitante": {
          "cnpj": "33.683.111/0001-07",
          "nome": "Secretaria de Governo Digital",
          "servico": "AppGovBr"
         },
        "cpf": "01534562567",
        "motivo": "prova de vida para obtenção de selo",
        "tipo": "B",
        "selogovbr_reuso_em": "999999",
        "expiracao_em": "120",
        "mensagem_falha": "Não foi possível confirmar a prova de vida, volte ao sistema XYZ para obter mais informações",
        "mensagem_sucesso": "Sua prova de vida foi realizada com sucesso, volte ao sistema XYZ para continuar o processo de autorização",
        "categoria": "PV"
  } 

Resultados esperados do Acesso à Transação da Prova de Vida
-----------------------------------------------------------

A transação retornará, em caso de autorização automática com selo, no formato JSON, as informações conforme exemplo:

Response: **201**

.. figure:: _images/exemploRespReqVbeta3.png
   :align: center
   :alt: 


Caso o usuário realizar validação facial **antes** da data definida no atributo "**reusar_apartir**", a transação **não** é autorizada automaticamente, e retornará, no formato JSON as informações conforme exemplo:

Response: **201**

.. code-block:: JSON

  { 
       "id": "0a4f7059-78b3-1b16-8179-56713d547f8a",
       "solicitante": {
       "cnpj": "33.683.111/0001-07",
       "nome": "Secretaria de Governo Digital",
       "servico": "AppGovBr"
    },
       "cpf": "01534562567",
       "motivo": "solicitação de prova de vida para liberação de benefício",
       "tipo": "B",
       "criado_em": "2021-05-10T14:14:38.083677-03:00",
       "expiracao_em": "2021-05-10T16:14:38.083677-03:00",
       "selogovbr": {
    
       "reusar_apartir": "2021-04-10T14:38.083677-03:00",
       "disponivel": true,
       "data": "2021-03-15T15:34:51-03:00",
       "usado": false
    },
       "categoria": "PV"
  } 

No exemplo acima, como a transação **não** foi autorizada automaticamente, o JSON retornado **não** apresenta o atributo RESPOSTA.

Obter dados usando id das Transações
------------------------------------

É possível fazer requisição para obter dados das Transações da Prova de vida usando o **id** (*UUID*) retornado pelo serviço:

-  https://h.meugov.estaleiro.serpro.gov.br/api/vbeta3/transacoes

Para acessar o serviço que disponibiliza os dados vinculados a uma determinada transação, a aplicação cliente deverá realizar uma requisição por meio do método GET à URL:
https://h.meugov.estaleiro.serpro.gov.br/api/vbeta3/transacoes/{idtransacao}

Exemplo de requisição:

.. code-block:: console

  https://h.meugov.estaleiro.serpro.gov.br/api/vbeta3/transacoes/0a4f7059-78b3-1b16-8179-5746089d7fb7


Parâmetros para GET https://h.meugov.estaleiro.serpro.gov.br/api/vbeta3/transacoes/{idtransacao}

============================  ======================================================================
**Variável**                  **Descrição**
----------------------------  ----------------------------------------------------------------------
**Authorization**             No *header*, palavra **Bearer** e o *acess_token* da requisição POST do https://h.meugov.estaleiro.serpro.gov.br/auth/oauth/token?grant_type=client_credentials
**idtransação**               **id** (*UUID*) da transação de prova de vida
============================  ======================================================================

Exemplos de Resultado:


- O atributo RESPOSTA do código JSON abaixo indica que o usuário já respondeu a autorização e realizou a validação facial com sucesso. Caso o usuário **não** tivesse respondido a autorização, o atributo RESPOSTA **não** estaria presente.


Response: **200**

.. code-block:: JSON

  { 
    "id": "fb5g8247-95c1-2f23-9580-6813178c9bf8",
       "solicitante": {
       "cnpj": "33.683.111/0001-07",
       "nome": "Secretaria de Governo Digital",
       "servico": "AppGovBr"
    },
       "cpf": "01534562567",
       "motivo": "solicitação de prova de vida para liberação de benefício",
       "tipo": "B",
       "criado_em": "2021-05-10T14:14:38.083677-03:00",
       "selogovbr": {
    
       "reusar_apartir": "2021-04-10T14:14.083677-03:00",
       "disponivel": true,
       "data": "2021-05-23T15:34:51-03:00",
       "usado": true
    },
       "resposta": {
       "autorizado": true,
       "data": "2021-05-23T15:34:51-03:00"
      },
     "expiracao_em": "2021-06-10T16:14:38.083677-03:00",
     "categoria": "PV"
  } 


No App "GovBr", a transação da prova de vida também pode ser negada. O motivo da negação pode ser porque o usuário **não** autorizou a validação facial ou porque ele **não** passou na validação. Caso o usuário não autorizar a validação facial, a transação retornará, no formato JSON, as informações conforme exemplo:

Response: **200**

.. code-block:: JSON

  { 
    "id": "fb5g8247-95c1-2f23-9580-6813178c9bf8",
       "solicitante": {
       "cnpj": "33.683.111/0001-07",
       "nome": "Secretaria de Governo Digital",
       "servico": "AppGovBr"
    },
       "cpf": "01534562567",
       "motivo": "solicitação de prova de vida para liberação de benefício",
       "tipo": "B",
       "criado_em": "2021-05-10T14:14:38.083677-03:00",
       "selogovbr": {
    
       "reusar_apartir": "2021-04-10T14:14.083677-03:00",
       "disponivel": true,
       "data": "2021-03-23T15:34:51-03:00",
       "usado": false
    },
       "resposta": {
       "autorizado": false,
       "data": "2021-05-10T15:37:38.083677-03:00",
       "motivo_negacao": 1
      },
    "expiracao_em": "2021-06-10T14:14:38.083677-03:00",
    "categoria": "PV"
  }

O valor do atributo "**motivo_negacao**" é um número de 1 a 4. Abaixo estão os motivos de cada número: 

1. Usuário escolheu não autorizar;
2. Falha na validação biometria Facial;
3. Falha na validação dados biográficos;
4. Falha na validação de dados biometricos e biográficos.

Enviar mensagens para o usuário
-------------------------------

Para acessar o serviço que envia mensagem ao usuário, a aplicação cliente deverá realizar uma requisição por meio do método POST à URL:
https://h.meugov.estaleiro.serpro.gov.br/api/vbeta1/mensagens

Parâmetros do Header para POST https://h.meugov.estaleiro.serpro.gov.br/api/vbeta1/mensagens

============================  ======================================================================
**Variável**                  **Descrição**
----------------------------  ----------------------------------------------------------------------
**Authorization**             Palavra **Bearer** e o *acess_token* da requisição POST do https://h.meugov.estaleiro.serpro.gov.br/auth/oauth/token?grant_type=client_credentials
**Content-Type**              Tipo do conteúdo da requisição que está sendo enviada. Nesse caso estamos enviando como um *application/json*
============================  ======================================================================

Parâmetros do Body para POST https://h.meugov.estaleiro.serpro.gov.br/api/vbeta1/mensagens

.. code-block:: JSON

  { 
  "remetente": {
    "cnpj": "(CNPJ do orgão dono da aplicação cliente.)",
    "nome": "(Nome do Orgão.)"
  },
  "titulo": "(Título da mensagem a ser enviada para o usuário.)",
  "conteudo": "(Conteúdo da mensagem.)",
  "tipo": "(Tipo da mensagem. Valor 'D' envia para um cpf específico, valor 'B' para broadcast)",
  "cpf": "(CPF do usuário para o qual deseja enviar a mensagem.)"
  } 


Ao chamar o serviço, a mensagem é enviada para o usuário, que recebe via *push notification* no aplicativo "GovBr". A mensagem pode ser enviada diretamente ao cidadão (CPF) ou enviada para todos (*broadcast*). Caso seja enviada para **todos**, o parâmetro “**cpf**” não deve ser informado na requisição.

O serviço retornará, em caso de sucesso, o código que identifica unicamente a mensagem (**UUID**), conforme exemplo:

Response: **201**

**Body**

{"7f000101-729a-1bab-8172-9a9c74160001"}

A aplicação cliente, utilizando determinados serviços, pode utilizar o **id** da mensagem para receber informações sobre a mesma ou para deletá-la.

Exemplos de requisição:

* Recebe informações de mensagem enviada
  
  - GET https://h.meugov.estaleiro.serpro.gov.br/api/vbeta1/mensagens/{id}

.. raw:: html
    
   <br>  

* Deleta mensagem enviada

  - DELETE https://h.meugov.estaleiro.serpro.gov.br/api/vbeta1/mensagens/{id}


Resultados Esperados e Erros do Acesso aos Serviços da Prova de Vida
---------------------------------------------------------------------

Como visto anteriormente, os acessos aos serviços (transações) da Prova de Vida ocorrem por meio de chamadas de URLs e as respostas são códigos presentes conforme padrão do protocolo HTTP por meio do retorno JSON. O retorno mostra o código de sucesso ou de erro e a respectiva descrição.

Exemplos de códigos HTTP de sucesso:

- **200**: Sucesso
- **201**: Dado cadastrado com Sucesso, retornando o ID do dado

.. raw:: html
    
   <br>  

Exemplos de códigos HTTP de erro:

- **400**: Algum dado informado incorretamente. Exemplo:

.. code-block:: JSON

  { 
  "status": "BAD_REQUEST",
  "message": "Argumentos não válidos",
  "errors": {
    "cpf": "número do registro de contribuinte individual brasileiro (CPF) inválido"
    }
  } 

- **401**: Usuário não autenticado
- **422**: Erro de validação na requisição. Exemplo:

.. code-block:: JSON

  { 
  "timestamp": "2021-05-10T14:14:38.083677-03:00",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "A não é um tipo válido [B,C]", 
  "path": "/vbeta1/transacoes"
  } 



.. |site externo| image:: _images/site-ext.gif
.. _`codificador para Base64`: https://www.base64decode.org/
.. _`OAuth 2.0`: https://oauth.net/2/
.. _`Login Único`: https://manual-roteiro-integracao-login-unico.servicos.gov.br/pt/stable/index.html



  

