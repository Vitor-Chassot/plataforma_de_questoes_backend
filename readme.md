# Plataforma de Questões Backend

Backend em Java + Spring Boot para uma plataforma de questões de matemática.

---

## 🚀 Funcionalidades

- Cadastro de usuários  
- Login com token (armazenado como hash no banco)  
- Logout  
- Recuperação de senha por e-mail (fluxo seguro em 3 etapas)  
- Seleção de questões por assunto e dificuldade  
- Expiração de tokens  

---

## 🧰 Tecnologias

- Java  

- Spring Boot  
- Spring Data JPA  
- Spring Security  
- PostgreSQL  
- Supabase  
- JavaMailSender  
- BCrypt (hash de senha)  
- HMAC-SHA256 (hash de tokens)

---

## 🌐 Base URL

Produção:  
https://plataforma-de-questoes-backend.onrender.com

---

## 🔐 Autenticação

O sistema **NÃO usa JWT**.

- Token é gerado no login (UUID)  
- Retornado ao cliente  
- Armazenado como **hash (HMAC-SHA256)** no banco  
- Validado comparando hashes  

📌 O token deve ser enviado manualmente (query params ou body).

---

## 📡 Endpoints

---

### 👤 Cadastro

**POST** `/api/usuarios`

#### Body (DTO: `UsuarioCadastroRequest`)
~~~json
{
  "nome": "SEU_NOME",
  "email": "email@email.com",
  "senha": "SUA_SENHA"
}
~~~

#### Exemplo:
~~~bash
curl -X POST https://plataforma-de-questoes-backend.onrender.com/api/usuarios \
-H "Content-Type: application/json" \
-d '{"nome":"SEU_NOME","email":"email@email.com","senha":"SUA_SENHA"}'
~~~

---

### 🔑 Login

**POST** `/api/auth/login`

#### Body (DTO: `LoginRequest`)
~~~json
{
  "email": "email@email.com",
  "senha": "SUA_SENHA"
}
~~~

#### Resposta:
~~~
TOKEN
~~~

---

### 🚪 Logout

**POST** `/api/auth/logout`

#### Body (DTO: `LogoutRequest`)
~~~json
{
  "email": "email@email.com",
  "token": "TOKEN"
}
~~~

---

### 📚 Buscar questões

**GET** `/api/me`

#### Query Params:
- `email`
- `token`
- `assunto`
- `dificuldade`

#### Exemplo:
~~~bash
curl -G https://plataforma-de-questoes-backend.onrender.com/api/me \
--data-urlencode "email=email@email.com" \
--data-urlencode "token=TOKEN" \
--data-urlencode "assunto=GEOMETRIA_PLANA" \
--data-urlencode "dificuldade=MEDIO"
~~~

#### Regras:
- Token expira em **3600 segundos (1 hora)**  
- Token inválido → `401`  
- Token expirado → removido do banco  

---

## 🔁 Recuperação de senha

### 1️⃣ Solicitar código

**POST** `/api/auth/novasenha/codigo`

#### Body (DTO: `PasswordSolicitarCodigoRequest`)
~~~json
{
  "email": "email@email.com"
}
~~~

📌 Um código de 6 dígitos é enviado por e-mail.

---

### 2️⃣ Validar código

**POST** `/api/auth/novasenha/token`

#### Body (DTO: `PasswordSolicitarTokenRequest`)
~~~json
{
  "email": "email@email.com",
  "codigo": "123456"
}
~~~

#### Resposta:
~~~
TOKEN
~~~

#### Regras:
- Código expira em **300 segundos (5 minutos)**

---

### 3️⃣ Definir nova senha

**POST** `/api/auth/novasenha/cadastro`

#### Body (DTO: `PasswordCadastrarRequest`)
~~~json
{
  "email": "email@email.com",
  "token": "TOKEN",
  "senha": "NOVA_SENHA",
  "confirmarSenha": "NOVA_SENHA"
}
~~~

#### Regras:
- Token expira em **300 segundos (5 minutos)**

---

## ⚠️ Observações importantes

- Tokens são armazenados como **hash (HMAC-SHA256)**  
- Não é possível recuperar o token original pelo banco  
- Apenas comparação de hash é utilizada  
- Múltiplos tokens de sessão podem existir por usuário  
- Tokens são invalidados manualmente no logout  
- Tokens expiram com base em tempo  

---

## 🧠 Design de segurança

- Senhas → BCrypt  
- Tokens de sessão → HMAC-SHA256  
- Reset de senha:
  - Código de 6 dígitos (email)  
  - Token UUID temporário  
- Expiração baseada em tempo (`LocalDateTime`)  
- Remoção de tokens expirados durante validação  

---

## 📌 Enums

### Assunto

- PROGRESSOES  
- CONJUNTOS_NUMERICOS  
- VARIAVEIS_E_FUNCOES  
- ANALISE_COMBINATORIA_PROBABILIDADE_ESTATISTICA  
- POLINOMIOS  
- LOGARITMO_E_EXPONENCIAL  
- GEOMETRIA_PLANA  
- GEOMETRIA_ESPACIAL  
- TRIGONOMETRIA  

---

### Dificuldade

- FACIL  
- MEDIO  
- DIFICIL  

---

## 🚀 Observação final

Este projeto utiliza:

**token stateful + hash + validação manual**

Diferente de JWT, porém:

- Mais simples para MVP  
- Controle total sobre sessões  
- Permite invalidação imediata  