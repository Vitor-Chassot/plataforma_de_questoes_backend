package com.vitor.plataforma_de_questoes_backend;

import jakarta.persistence.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.Duration;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class Constants {
    public static final int MAX_SECONDS_TOKEN_SESSION = 3600;
    public static final int MAX_SECONDS_CODE_PASSWORD= 300;
    public static final int MAX_SECONDS_TOKEN_PASSWORD= 300;

}

@Converter(autoApply = false)
class EnumPostgresConverter<E extends Enum<E>>
        implements AttributeConverter<E, String> {

    @Override
    public String convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public E convertToEntityAttribute(String dbData) {
        throw new UnsupportedOperationException(
                "Use converter específico por enum"
        );
    }
}

@Converter(autoApply = false)
class TipoAssuntoConverter
        implements AttributeConverter<TipoAssunto, String> {

    @Override
    public String convertToDatabaseColumn(TipoAssunto attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public TipoAssunto convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TipoAssunto.valueOf(dbData);
    }
}

@Converter(autoApply = false)
class TipoNivelConverter
            implements AttributeConverter<TipoNivel, String> {

    @Override
    public String convertToDatabaseColumn(TipoNivel attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public TipoNivel convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TipoNivel.valueOf(dbData);
    }
}


class TokenHasher {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String SECRET = System.getenv("TOKEN_SECRET");

    public static String hash(String token) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(SECRET.getBytes(), ALGORITHM));
            byte[] result = mac.doFinal(token.getBytes());

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(result);

        } catch (Exception e) {
            throw new IllegalStateException("Erro ao gerar hash do token", e);
        }
    }
}

//autentication: /////////////////////////////////////////////////////////////////////////////
@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final UsuarioRepository usuarioRepo;
    private final AuthTokenRepository tokenRepo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthController(UsuarioRepository usuarioRepo, AuthTokenRepository tokenRepo) {
        this.usuarioRepo = usuarioRepo;
        this.tokenRepo = tokenRepo;
    }
    @Transactional
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {

        var usuarioOpt = usuarioRepo.findByEmail(req.email);

        if (usuarioOpt.isEmpty()) {
            return ResponseEntity
                    .status(401)
                    .body("Email ou senha incorretos. Esqueceu a senha?\n");
        }

        Usuario usuario = usuarioOpt.get();

        if (!encoder.matches(req.senha, usuario.getSenhaHash())) {
            return ResponseEntity
                    .status(401)
                    .body("Email ou senha incorretos. Esqueceu a senha?\n");
        }
        // gera token simples
        String token = java.util.UUID.randomUUID().toString();
        String token_hash = TokenHasher.hash(token);
        tokenRepo.save(new AuthToken(usuario, token_hash));

        return ResponseEntity.ok(token+"\n");
    }


    @Transactional
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestBody LogoutRequest req
    ) {
        var usuario=usuarioRepo.findByEmail(req.email);
        String token_hash = TokenHasher.hash(req.token);
        List<AuthToken> authTokenList=tokenRepo.findAllByUsuario(usuario.get());
        for(AuthToken authToken:authTokenList) {
            if(token_hash.equals(authToken.getTokenHash())) {
                tokenRepo.delete(authToken);
                return ResponseEntity.ok("Logout realizado com sucesso\n");
            }
        }

        return ResponseEntity.status(401).body("Requisição Invalida\n");



    }
}

@Entity
@Table(name = "auth_token", schema = "educa")
class AuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    public Usuario getUsuario() {
        return usuario;
    }
    @Column(nullable = false, unique = true)
    private String token_hash;

    public LocalDateTime getLocalDateTime() {
        return criadoEm ;
    }
    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();

    public AuthToken() {}

    public AuthToken(Usuario usuario, String token_hash) {
        this.usuario = usuario;
        this.token_hash = token_hash;
    }

    public String getTokenHash() {
        return token_hash;
    }
}

interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {
    Optional<AuthToken> findByTokenHash(String token);
    Optional<AuthToken> findByUsuario(Optional<Usuario> usuario);
    void deleteByTokenHash(String token);
    void deleteByUsuario(Usuario usuario);
    boolean existsByTokenHash(String token_hash);
    List<AuthToken> findAllByUsuario(Usuario usuario);
}

class LoginRequest {
    public String email;
    public String senha;
}

class LogoutRequest {
    public String email;
    public String token;
}
//////////////////////////////////////////////////////////

//cadastro: ////////////////////////////////////////////////////////

@RestController
@RequestMapping("/api/usuarios")
class UsuarioController {

    private final UsuarioRepository repo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UsuarioController(UsuarioRepository repo) {
        this.repo = repo;
    }

    @PostMapping
    public ResponseEntity<?> cadastrar(@RequestBody UsuarioCadastroRequest req) {

        // 1️⃣ valida email duplicado
        if (repo.existsByEmail(req.email)) {
            return ResponseEntity
                    .status(409) // Conflict
                    .body("Já existe usuário com este email.\n");
        }

        // 2️⃣ criar entidade
        Usuario u = new Usuario();
        u.setNome(req.nome);
        u.setEmail(req.email);
        u.setSenhaHash(encoder.encode(req.senha));
        u.setCriadoEm(java.time.LocalDateTime.now());

        // 3️⃣ salvar
        repo.save(u);

        return ResponseEntity.status(201).body("Usuário criado com sucesso!\n");
    }
}

interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    boolean existsByEmail(String email);
    Optional<Usuario> findByEmail(String email);

}

class UsuarioCadastroRequest {
    public String nome;
    public String email;
    public String senha;
}


@Entity
@Table(name = "usuario", schema = "educa")
class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    public Usuario() {}

    public Usuario(String nome, String email, String senhaHash) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
/////////////////////////////////////////////////////////////////////

//mudança de senha: //////////////////////////////////////////////////////////////////////
interface NewPasswordTokenRepository extends JpaRepository<NewPasswordToken, Long> {
    Optional<NewPasswordToken> findByTokenHash(String token_hash);
    Optional<NewPasswordToken> findByUsuario(Usuario usuario);
    boolean existsByUsuario(Usuario usuario);
    boolean existsByCodigoHash(String codigo_hash);
    boolean existsByTokenHash(String token_hash);
    void deleteByTokenHash(String token_hash);
    void deleteByUsuario(Usuario usuario);
    @Override
    void flush();
}


@RestController
@RequestMapping("/api/auth")
class PasswordResetController {

    private final UsuarioRepository usuarioRepo;
    private final NewPasswordTokenRepository tokenRepo;
    private final JavaMailSender mailSender;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Autowired
    public PasswordResetController(
            UsuarioRepository usuarioRepo,
            NewPasswordTokenRepository tokenRepo,
            JavaMailSender mailSender
    ) {
        this.usuarioRepo = usuarioRepo;
        this.tokenRepo = tokenRepo;
        this.mailSender = mailSender;
    }

    @Transactional
    @PostMapping("/novasenha/codigo")
    public ResponseEntity<?> solicitarNovaSenha(
            @RequestBody PasswordSolicitarCodigoRequest req
    ) {

        // 1️⃣ verifica se usuário existe
        Usuario usuario = usuarioRepo.findByEmail(req.email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Usuário não encontrado\n"
                        )
                );

        // 2️⃣ remove token antigo (se existir)
        if (tokenRepo.existsByUsuario(usuario)) {
            tokenRepo.deleteByUsuario(usuario);
            tokenRepo.flush();
        }

        // 3️⃣ gera código de 6 dígitos (seguro)
        SecureRandom random = new SecureRandom();
        String codigo = String.format("%06d", random.nextInt(1_000_000));
        String codigo_hash=TokenHasher.hash(codigo);


        tokenRepo.save(new NewPasswordToken(usuario, codigo_hash));

        // 4️⃣ envia e-mail
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(usuario.getEmail());
            message.setSubject("Solicitação de redefinição de senha");
            message.setText(
                    "Olá " + usuario.getNome() + ",\n\n" +
                            "Use o código abaixo para redefinir sua senha:\n\n" +
                            codigo + "\n\n" +
                            "Se você não solicitou isso, ignore este e-mail."
            );

            mailSender.send(message);

        } catch (Exception e) {
            // força rollback
            throw new RuntimeException("Erro ao enviar e-mail\n", e);
        }

        // 5️⃣ resposta segura
        return ResponseEntity.ok(
                "Código enviado para o seu e-mail.\n"
        );
    }
    @Transactional
    @PostMapping("/novasenha/token")
    public ResponseEntity<?> solicitarNovaSenha(
            @RequestBody PasswordSolicitarTokenRequest req
    ) {

        // 1️⃣ verifica se usuário existe
        Usuario usuario = usuarioRepo.findByEmail(req.email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Usuário não encontrado\n"
                        )
                );


        if (!tokenRepo.existsByUsuario(usuario)) {
            return ResponseEntity.status(401).body("Requisição não encontrada, solicite novo código!\n");
        }

        var tokenOpt=tokenRepo.findByUsuario(usuario);
        String codigo_hash = TokenHasher.hash(req.codigo);
        if(!codigo_hash.equals(tokenOpt.get().getCodigoHash())) {
            return ResponseEntity.status(401).body("Código Inválido, digite outro código!\n");
        }

        LocalDateTime data=tokenOpt.get().getLocalDateTime();
        LocalDateTime agora = LocalDateTime.now();

        Duration duracao = Duration.between(data, agora);

        long segundos = duracao.getSeconds();

        if (segundos > Constants.MAX_SECONDS_CODE_PASSWORD) {
            tokenRepo.deleteByUsuario(usuario);
            return ResponseEntity.status(401).body("Codigo expirado, solicite novo codigo!\n");
        }
        String token = UUID.randomUUID().toString();
        String token_hash = TokenHasher.hash(token);
        tokenOpt.get().setTokenHash(token_hash);
        tokenOpt.get().setLocalDateTime(LocalDateTime.now());
        tokenRepo.save(tokenOpt.get());
        return ResponseEntity.ok(
                "TOKEN: " + token +"\n"
        );

    }
    @Transactional
    @PostMapping("/novasenha/cadastro")
    public ResponseEntity<?> solicitarNovaSenha(
            @RequestBody PasswordCadastrarRequest  req
    ) {
        var usuario=usuarioRepo.findByEmail(req.email);
        var tokenOpt=tokenRepo.findByUsuario(usuario.get());
        String token_hash= TokenHasher.hash(req.token);
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Nao Autorizado\n");
        }
        if(!token_hash.equals(tokenOpt.get().getTokenHash())) {
            return ResponseEntity.status(401).body("Token Invalido!\n");
        }
        if(!req.senha.equals(req.confirmarSenha)) {
            return ResponseEntity.status(401).body("Senhas não sãos iguais!\n");
        }



        LocalDateTime data=tokenOpt.get().getLocalDateTime();
        LocalDateTime agora = LocalDateTime.now();

        Duration duracao = Duration.between(data, agora);

        long segundos = duracao.getSeconds();

        if (segundos > Constants.MAX_SECONDS_TOKEN_PASSWORD) {
            tokenRepo.deleteByUsuario(usuario.get());
            return ResponseEntity.status(401).body("Requisição expirada, solicite novo codigo!\n");
        }
        usuario.get().setSenhaHash(encoder.encode(req.senha));
        usuarioRepo.save(usuario.get());
        tokenRepo.deleteByUsuario(usuario.get());
        // 5️⃣ resposta segura
        return ResponseEntity.ok(
                "Nova senha configurada com sucesso!.\n"
        );

    }
}

class PasswordSolicitarTokenRequest {
    public String email;
    public String codigo;
}

class PasswordCadastrarRequest {
    public String email;
    public String token;
    public String senha;
    public String confirmarSenha;
}

class PasswordSolicitarCodigoRequest {
    public String email;
}


@Entity
@Table(name = "new_password_token", schema = "educa")
class NewPasswordToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false,unique = true)
    private Usuario usuario;

    public Usuario getUsuario() {
        return usuario;
    }

    @Column(nullable = false)
    private String codigo_hash;


    public String getCodigoHash() {
        return codigo_hash;
    }

    @Column(unique = true)
    private String token_hash;


    public LocalDateTime getLocalDateTime() {
        return atualizadoEm ;
    }
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm = LocalDateTime.now();
    public void setLocalDateTime(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
    public NewPasswordToken() {}

    public NewPasswordToken(Usuario usuario, String codigo_hash) {
        this.usuario = usuario;
        this.codigo_hash=codigo_hash;
    }
    public void setTokenHash(String token_hash) {
        this.token_hash = token_hash;
    }
    public String getTokenHash() {
        return token_hash;
    }
}

//questoes: //////////////////////////////////////////////////

interface QuestaoRepository extends JpaRepository<Questao, Long> {

    @Query(value = """
        SELECT * FROM educa.questao q
        WHERE q.assunto = CAST(:assunto AS tipo_assunto)
          AND q.dificuldade = CAST(:dificuldade AS tipo_nivel)
    """, nativeQuery = true)
    List<Questao> buscar(
            @Param("assunto") String assunto,
            @Param("dificuldade") String dificuldade
    );
}

class QuestionarioRequest{
    public String email;
    public String token;
    public String assunto;
    public String dificuldade;

}
class QuestaoResponseDTO {
    private String conteudo;
    private Integer ano;
    private Integer numeroQuestao;

    public QuestaoResponseDTO(String conteudo, Integer ano, Integer numeroQuestao) {
        this.conteudo = conteudo;
        this.ano = ano;
        this.numeroQuestao = numeroQuestao;
    }

    public String getConteudo() {
        return conteudo;
    }

    public Integer getAno() {
        return ano;
    }

    public Integer getNumeroQuestao() {
        return numeroQuestao;
    }
}

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/me")
@Transactional
class MeController {
    private final QuestaoRepository questaoRepo;
    private final AuthTokenRepository tokenRepo;
    private final UsuarioRepository usuarioRepo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public MeController(UsuarioRepository usuarioRepo,AuthTokenRepository tokenRepo, QuestaoRepository questaoRepo) {
        this.tokenRepo = tokenRepo;
        this.questaoRepo=questaoRepo;
        this.usuarioRepo=usuarioRepo;
    }
    @GetMapping
    public ResponseEntity<?> me(
            @RequestParam String email,
            @RequestParam String token,
            @RequestParam String assunto,
            @RequestParam String dificuldade
    ) {
        String token_hash = TokenHasher.hash(token);
        var usuario=usuarioRepo.findByEmail(email);
        List<AuthToken> lisAuthToken = tokenRepo.findAllByUsuario(usuario.get());
        boolean tokenValido=false;
        AuthToken authToken=null;
        for(AuthToken authTokenAux:lisAuthToken) {
            if(token_hash.equals(authTokenAux.getTokenHash())){
                tokenValido=true;
                authToken=authTokenAux;
            }
        }
        if (!tokenValido) {
            return ResponseEntity.status(401).body("Requisição Invalida\n");
        }
        LocalDateTime data=authToken.getLocalDateTime();
        LocalDateTime agora = LocalDateTime.now();

        Duration duracao = Duration.between(data, agora);

        long segundos = duracao.getSeconds();

        if (segundos > Constants.MAX_SECONDS_TOKEN_SESSION) {
            tokenRepo.deleteByTokenHash(token_hash);
            return ResponseEntity.status(401).body("Requisicao expirada, faça login novamente!\n");
        }

        var questoes = questaoRepo.buscar(
                assunto,
                dificuldade
        );


        List<QuestaoResponseDTO> response = questoes.stream()
                .map(q -> new QuestaoResponseDTO(
                        q.getConteudo(),
                        q.getAno(),
                        q.getNumeroQuestao()
                ))
                .toList();
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(response);

        return ResponseEntity.ok(json+"\n");





    }
}

enum TipoNivel {
    FACIL, MEDIO, DIFICIL
}

enum TipoAssunto {
    PROGRESSOES,
    CONJUNTOS_NUMERICOS,
    VARIAVEIS_E_FUNCOES,
    ANALISE_COMBINATORIA_PROBABILIDADE_ESTATISTICA,
    POLINOMIOS,
    LOGARITMO_E_EXPONENCIAL,
    GEOMETRIA_PLANA,
    GEOMETRIA_ESPACIAL,
    TRIGONOMETRIA

}


@Entity
@Table(name = "questao", schema = "educa")
class Questao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String conteudo;

    @Convert(converter = TipoAssuntoConverter.class)
    @Column(name = "assunto", columnDefinition = "tipo_assunto")
    private TipoAssunto assunto;

    @Convert(converter = TipoNivelConverter.class)
    @Column(name = "dificuldade", columnDefinition = "tipo_nivel")
    private TipoNivel dificuldade;

    @Column(length = 1)
    private String resposta;

    @Column(name = "ano")
    private int ano;
    @Column(name = "numero_questao")
    private int numeroQuestao;
    // ✅ GETTERS
    public Long getId() { return id; }
    public String getConteudo() { return conteudo; }
    public TipoAssunto getAssunto() { return assunto; }
    public TipoNivel getDificuldade() { return dificuldade; }
    public String getResposta() { return resposta; }
    public int getAno() { return ano; }
    public int getNumeroQuestao() { return numeroQuestao; }
    // ✅ (opcional) setters se precisar
}
//////////////////////////////////////////////////////////////////////

@SpringBootApplication
public class Main {

    public static void main(String[] args) {

        SpringApplication.run(Main.class, args);

    }
    /*
    @Bean
    CommandLineRunner init(UsuarioRepository repo) {
        return args -> {

            if (repo.count() == 1) {
                Usuario u = new Usuario();
                u.setNome("Teste2");
                u.setEmail("teste2@email.com");

                var encoder = new BCryptPasswordEncoder();
                u.setSenhaHash(encoder.encode("123456"));

                repo.save(u);
                System.out.println("Usuário inserido com sucesso!");
            } else {
                System.out.println("Usuário já existente, não vou duplicar 👍");
            }
        };
    }*/
}

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**") // 👈 ESSENCIAL
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            "/api/usuarios",
                            "/api/auth/login",
                            "/api/auth/logout",
                            "/api/me",   //token
                            "/api/auth/novasenha/codigo",
                            "/api/auth/novasenha/token",  //codigo
                            "/api/auth/novasenha/cadastro"  //token
                    ).permitAll()
                    .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults());

        return http.build();

    }
}



