# Plataforma de Questões Backend

Backend em **Java + Spring Boot** para uma plataforma de questões de matemática.  
O sistema oferece:

- cadastro de usuários
- login com token simples persistido no banco
- logout
- recuperação de senha por e-mail
- seleção de questões por assunto e dificuldade

## Tecnologias

- Java
- Spring Boot
- Spring Data JPA
- Spring Security
- PostgreSQL
- Supabase
- JavaMailSender

## Base URL

Em produção:

```
https://plataforma-de-questoes-backend.onrender.com
```

## Autenticação

Header:

```
Authorization: Bearer TOKEN
```

---

# Endpoints

## Cadastro

```
curl -X POST https://plataforma-de-questoes-backend.onrender.com/api/usuarios \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "SEU_NOME",
    "email": "email@email.com",
    "senha": "NOVA_SENHA"
  }'
```

---

## Login

```
curl -X POST https://plataforma-de-questoes-backend.onrender.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "email@email.com",
    "senha": "SUA_SENHA"
  }'
```

Resposta:
```
TOKEN
```

---

## Logout

```
curl -X POST https://plataforma-de-questoes-backend.onrender.com/api/auth/logout \
  -H "Authorization: Bearer TOKEN"
```

---

## Buscar questões

```
curl -G https://plataforma-de-questoes-backend.onrender.com/api/me \
  --data-urlencode "email=email@email.com" \
  --data-urlencode "token=TOKEN" \
  --data-urlencode "assunto=GEOMETRIA_PLANA" \
  --data-urlencode "dificuldade=MEDIO"
```

---

## Nova senha (código)

```
curl -X POST https://plataforma-de-questoes-backend.onrender.com/api/auth/novasenha/codigo \
  -H "Content-Type: application/json" \
  -d '{
    "email": "email@email.com"
  }'
```

---

## Nova senha (validar código)

```
curl -X POST https://plataforma-de-questoes-backend.onrender.com/api/auth/novasenha/token \
  -H "Content-Type: application/json" \
  -d '{
    "email": "email@email.com",
    "codigo": "SEU_CODIGO"
  }'
```

---

## Nova senha (definir)

```
curl -X POST https://plataforma-de-questoes-backend.onrender.com/api/auth/novasenha/cadastro \
  -H "Content-Type: application/json" \
  -d '{
    "token": "TOKEN",
    "senha": "NOVA_SENHA",
    "confirmarSenha": "NOVA_SENHA"
  }'
```

---

# Observações

- Token não é JWT (é salvo no banco)
- Login retorna apenas string
- `/api/me` valida email + token
- Use valores exatos dos enums
