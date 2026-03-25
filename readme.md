# Plataforma de Questões Backend

Backend em Java + Spring Boot para uma plataforma de questões de matemática.

---

## 🚀 Funcionalidades

- Cadastro de usuários
- Login com token de sessão
- Logout
- Recuperação de senha por e-mail em 3 etapas
- Seleção de questões por assunto e dificuldade
- Expiração de tokens por tempo
- Hash de senha com BCrypt
- Hash de token com HMAC-SHA256

---

## 🧰 Tecnologias

- Java
- Spring Boot
- Spring Data JPA
- Spring Security
- PostgreSQL
- Supabase
- JavaMailSender
- BCrypt
- HMAC-SHA256

---

## 🌐 Base URL

Produção:

```bash
https://plataforma-de-questoes-backend.onrender.com
```

---

## 🔐 Autenticação

Este sistema **não usa JWT**.

Fluxo:

1. O usuário faz login.
2. O backend gera um token UUID.
3. O token é retornado ao cliente.
4. O token é salvo no banco apenas como **hash HMAC-SHA256**.
5. Nas próximas requisições, o cliente envia o token manualmente.

---

## 📡 Endpoints

---

### 👤 Cadastro de usuário

**POST** `/api/usuarios`

Cria um novo usuário.

#### Body
```json
{
  "nome": "SEU_NOME",
  "email": "email@email.com",
  "senha": "SUA_SENHA"
}
```

#### Comando completo
```bash
curl -X POST https://plataforma-de-questoes-backend.onrender.com/api/usuarios \
-H "Content-Type: application/json" \
-d '{"nome":"SEU_NOME","email":"email@email.com","senha":"SUA_SENHA"}'
```

#### Respostas possíveis
- `201 Created` → Usuário criado com sucesso
- `409 Conflict` → Já existe usuário com este e-mail

---

### 🔑 Login

**POST** `/api/auth/login`

Faz login com e-mail e senha. Retorna um token de sessão.

#### Body
```json
{
  "email": "email@email.com",
  "senha": "SUA_SENHA"
}
```

#### Comando completo
```bash
curl -X POST https://plataforma-de-questoes-backend.onrender.com/api/auth/login \
-H "Content-Type: application/json" \
-d '{"email":"email@email.com","senha":"SUA_SENHA"}'
```

#### Respostas possíveis
- `200 OK` → retorna o token em texto puro
- `401 Unauthorized` → e-mail ou senha incorretos

#### Exemplo da resposta
```bash
TOKEN_AQUI
```

---

### 🚪 Logout

**POST** `/api/auth/logout`

Remove o token de sessão informado.

#### Body
```json
{
  "email": "email@email.com",
  "token": "TOKEN"
}
```

#### Comando completo
```bash
curl -X POST https://plataforma-de-questoes-backend.onrender.com/api/auth/logout \
-H "Content-Type: application/json" \
-d '{"email":"email@email.com","token":"TOKEN"}'
```

#### Respostas possíveis
- `200 OK` → Logout realizado com sucesso
- `401 Unauthorized` → requisição inválida

---

### 📚 Buscar questões

**GET** `/api/me`

Busca questões filtradas por assunto e dificuldade, validando o token de sessão.

#### Query params
- `email`
- `token`
- `assunto`
- `dificuldade`

#### Comando completo
```bash
curl -G https://plataforma-de-questoes-backend.onrender.com/api/me \
--data-urlencode "email=email@email.com" \
--data-urlencode "token=TOKEN" \
--data-urlencode "assunto=GEOMETRIA_PLANA" \
--data-urlencode "dificuldade=MEDIO"
```

#### Resposta
Retorna um JSON em texto com uma lista de questões contendo:
- `conteudo`
- `ano`
- `numeroQuestao`

#### Exemplo de resposta
```json
[
  {
    "conteudo": "...",
    "ano": 2023,
    "numeroQuestao": 12
  }
]
```

#### Regras
- Token expira em `3600` segundos (`1 hora`)
- Token inválido → `401`
- Token expirado → o token é removido do banco

---

## 🔁 Recuperação de senha

O fluxo de recuperação de senha acontece em 3 etapas.

---

### 1️⃣ Solicitar código

**POST** `/api/auth/novasenha/codigo`

Envia um código de 6 dígitos para o e-mail do usuário.

#### Body
```json
{
  "email": "email@email.com"
}
```

#### Comando completo
```bash
curl -X POST https://plataforma-de-questoes-backend.onrender.com/api/auth/novasenha/codigo \
-H "Content-Type: application/json" \
-d '{"email":"email@email.com"}'
```

#### Respostas possíveis
- `200 OK` → Código enviado para o seu e-mail
- `404 Not Found` → Usuário não encontrado

---

### 2️⃣ Validar código

**POST** `/api/auth/novasenha/token`

Valida o código enviado por e-mail e gera um token temporário para redefinição de senha.

#### Body
```json
{
  "email": "email@email.com",
  "codigo": "123456"
}
```

#### Comando completo
```bash
curl -X POST https://plataforma-de-questoes-backend.onrender.com/api/auth/novasenha/token \
-H "Content-Type: application/json" \
-d '{"email":"email@email.com","codigo":"123456"}'
```

#### Respostas possíveis
- `200 OK` → retorna o token temporário
- `401 Unauthorized` → requisição não encontrada, código inválido ou código expirado
- `404 Not Found` → usuário não encontrado

#### Exemplo da resposta
```bash
TOKEN: SEU_TOKEN_TEMPORARIO
```

#### Regras
- O código expira em `300` segundos (`5 minutos`)

---

### 3️⃣ Definir nova senha

**POST** `/api/auth/novasenha/cadastro`

Salva a nova senha usando o token temporário gerado na etapa anterior.

#### Body
```json
{
  "email": "email@email.com",
  "token": "TOKEN",
  "senha": "NOVA_SENHA",
  "confirmarSenha": "NOVA_SENHA"
}
```

#### Comando completo
```bash
curl -X POST https://plataforma-de-questoes-backend.onrender.com/api/auth/novasenha/cadastro \
-H "Content-Type: application/json" \
-d '{"email":"email@email.com","token":"TOKEN","senha":"NOVA_SENHA","confirmarSenha":"NOVA_SENHA"}'
```

#### Respostas possíveis
- `200 OK` → Nova senha configurada com sucesso
- `401 Unauthorized` → token inválido, senha diferente ou requisição expirada

#### Regras
- O token expira em `300` segundos (`5 minutos`)
- As senhas `senha` e `confirmarSenha` precisam ser iguais

---

## 📌 Valores aceitos

### Assunto
- `PROGRESSOES`
- `CONJUNTOS_NUMERICOS`
- `VARIAVEIS_E_FUNCOES`
- `ANALISE_COMBINATORIA_PROBABILIDADE_ESTATISTICA`
- `POLINOMIOS`
- `LOGARITMO_E_EXPONENCIAL`
- `GEOMETRIA_PLANA`
- `GEOMETRIA_ESPACIAL`
- `TRIGONOMETRIA`

### Dificuldade
- `FACIL`
- `MEDIO`
- `DIFICIL`

---

## ⚠️ Observações importantes

- Senhas são armazenadas com **BCrypt**
- Tokens de sessão são armazenados apenas como **hash**
- Não é possível recuperar o token original pelo banco
- O logout remove o token manualmente
- Múltiplos tokens podem existir para o mesmo usuário
- Tokens expirados são removidos durante a validação
- O endpoint `/api/me` usa `GET` com query params, não `POST`

---

## 🧠 Design de segurança

- **Senha** → BCrypt
- **Token de sessão** → HMAC-SHA256
- **Reset de senha** →
  - código de 6 dígitos enviado por e-mail
  - token UUID temporário
- **Controle de tempo** baseado em `LocalDateTime`

---

## 🚀 Resumo rápido do fluxo

### Criar conta
```bash
curl -X POST https://plataforma-de-questoes-backend.onrender.com/api/usuarios \
-H "Content-Type: application/json" \
-d '{"nome":"SEU_NOME","email":"email@email.com","senha":"SUA_SENHA"}'
```

### Logar
```bash
curl -X POST https://plataforma-de-questoes-backend.onrender.com/api/auth/login \
-H "Content-Type: application/json" \
-d '{"email":"email@email.com","senha":"SUA_SENHA"}'
```

### Buscar questões
```bash
curl -G https://plataforma-de-questoes-backend.onrender.com/api/me \
--data-urlencode "email=email@email.com" \
--data-urlencode "token=TOKEN" \
--data-urlencode "assunto=GEOMETRIA_PLANA" \
--data-urlencode "dificuldade=MEDIO"
```

### Sair da sessão
```bash
curl -X POST https://plataforma-de-questoes-backend.onrender.com/api/auth/logout \
-H "Content-Type: application/json" \
-d '{"email":"email@email.com","token":"TOKEN"}'
```

---

## 📎 Observação final

Este backend foi pensado para ser simples de consumir no terminal e fácil de testar com `curl`, mantendo o fluxo explícito para cadastro, autenticação, busca de questões e recuperação de senha.
