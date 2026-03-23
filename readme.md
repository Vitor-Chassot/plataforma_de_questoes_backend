# Plataforma de Questões Backend

Backend em Java + Spring Boot para uma plataforma de questões de matemática.

## 🚀 Funcionalidades

- Cadastro de usuários
- Login com token (armazenado com hash no banco)
- Logout
- Recuperação de senha por e-mail
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
- BCrypt (hash de senha e token)

---

## 🌐 Base URL

Produção:

https://plataforma-de-questoes-backend.onrender.com

---

## 🔐 Autenticação

O sistema NÃO usa JWT.

- Token é gerado no login
- Retornado ao cliente
- Armazenado como hash (BCrypt)
- Validado via BCrypt.matches()

O token deve ser enviado manualmente (query ou body).

---

# 📡 Endpoints

## 👤 Cadastro

curl -X POST https://plataforma-de-questoes-backend.onrender.com/api/usuarios -H "Content-Type: application/json" -d '{"nome":"SEU_NOME","email":"email@email.com","senha":"NOVA_SENHA"}'

---

## 🔑 Login

curl -X POST https://plataforma-de-questoes-backend.onrender.com/api/auth/login -H "Content-Type: application/json" -d '{"email":"email@email.com","senha":"SUA_SENHA"}'

Resposta:
TOKEN

---

## 🚪 Logout

curl -X POST https://plataforma-de-questoes-backend.onrender.com/api/auth/logout -H "Content-Type: application/json" -d '{"email":"email@email.com","token":"TOKEN"}'

---

## 📚 Buscar questões

curl -G https://plataforma-de-questoes-backend.onrender.com/api/me \
  --data-urlencode "email=email@email.com" \
  --data-urlencode "token=TOKEN" \
  --data-urlencode "assunto=GEOMETRIA_PLANA" \
  --data-urlencode "dificuldade=MEDIO"

Regras:
- Token expira em 60 segundos
- Token inválido → 401
- Token expirado → removido do banco

---

## 🔁 Recuperação de senha

### 1️⃣ Solicitar código

curl -X POST https://plataforma-de-questoes-backend.onrender.com/api/auth/novasenha/codigo -H "Content-Type: application/json" -d '{"email":"email@email.com"}'

---

### 2️⃣ Validar código

curl -X POST https://plataforma-de-questoes-backend.onrender.com/api/auth/novasenha/token -H "Content-Type: application/json" -d '{"email":"email@email.com","codigo":"SEU_CODIGO"}'

Resposta:
TOKEN

---

### 3️⃣ Definir nova senha

curl -X POST https://plataforma-de-questoes-backend.onrender.com/api/auth/novasenha/cadastro -H "Content-Type: application/json" -d '{"token":"TOKEN","senha":"NOVA_SENHA","confirmarSenha":"NOVA_SENHA"}'

---

# ⚠️ Observações importantes

- Tokens são armazenados como hash (BCrypt)
- Não é possível buscar token diretamente no banco
- Validação feita com BCrypt.matches()
- Apenas 1 token ativo por usuário
- Tokens são invalidados no logout
- Tokens expiram automaticamente

---

# 🧠 Design de segurança

- Senhas → BCrypt
- Tokens → BCrypt
- Reset de senha:
  - código curto (email)
  - token UUID
- Expiração baseada em tempo (LocalDateTime)
- Remoção automática de tokens expirados

---

# 📌 Enums

## Assunto

- PROGRESSOES
- CONJUNTOS_NUMERICOS
- VARIAVEIS_E_FUNCOES
- ANALISE_COMBINATORIA_PROBABILIDADE_ESTATISTICA
- POLINOMIOS
- LOGARITMO_E_EXPONENCIAL
- GEOMETRIA_PLANA
- GEOMETRIA_ESPACIAL
- TRIGONOMETRIA

## Dificuldade

- FACIL
- MEDIO
- DIFICIL

---

## 🚀 Observação final

Este projeto usa:

token stateful + hash + validação manual

Diferente de JWT, mas mais simples e controlável para MVP.
