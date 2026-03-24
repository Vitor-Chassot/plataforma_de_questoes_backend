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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

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




interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    boolean existsByEmail(String email);
    Optional<Usuario> findByEmail(String email);
}
interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {
    Optional<AuthToken> findByTokenHash(String token);
    Optional<AuthToken> findByUsuario(Optional<Usuario> usuario);
    void deleteByTokenHash(String token);
    void deleteByUsuario(Usuario usuario);
    boolean existsByTokenHash(String token_hash);
}
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

interface NewPasswordTokenRepository extends JpaRepository<NewPasswordToken, Long> {
    Optional<NewPasswordToken> findByToken(String token);
    Optional<NewPasswordToken> findByUsuario(Usuario usuario);
    boolean existsByUsuario(Usuario usuario);
    boolean existsByCodigo(String codigo);
    boolean existsByToken(String token);
    void deleteByToken(String token);
    void deleteByUsuario(Usuario usuario);
    @Override
    void flush();
}
class LoginRequest {
    public String email;
    public String senha;
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
    private String codigo;


    public String getCodigo() {
        return codigo;
    }

    @Column(nullable = false, unique = true)
    private String token;


    public LocalDateTime getLocalDateTime() {
        return criadoEm ;
    }
    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();

    public NewPasswordToken() {}

    public NewPasswordToken(Usuario usuario, String codigo, String token) {
        this.usuario = usuario;
        this.codigo=codigo;
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
class UsuarioRequest {
    public String nome;
    public String email;
    public String senha;
}

class NovaSenhaTokenRequest {
    public String email;
    public String codigo;
}

class NovaSenhaCadastro {
    public String token;
    public String senha;
    public String confirmarSenha;
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



@RestController
@RequestMapping("/api/usuarios")
class UsuarioController {

    private final UsuarioRepository repo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UsuarioController(UsuarioRepository repo) {
        this.repo = repo;
    }

    @PostMapping
    public ResponseEntity<?> cadastrar(@RequestBody UsuarioRequest req) {

        // 1️⃣ valida email duplicado
        if (repo.existsByEmail(req.email)) {
            return ResponseEntity
                    .status(409) // Conflict
                    .body("Já existe usuário com este email.");
        }

        // 2️⃣ criar entidade
        Usuario u = new Usuario();
        u.setNome(req.nome);
        u.setEmail(req.email);
        u.setSenhaHash(encoder.encode(req.senha));
        u.setCriadoEm(java.time.LocalDateTime.now());

        // 3️⃣ salvar
        repo.save(u);

        return ResponseEntity.status(201).body("Usuário criado com sucesso!");
    }
}
class AuthTokenRequest {
    public String email;
    public String token;
}
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
                    .body("Email ou senha incorretos. Esqueceu a senha?");
        }

        Usuario usuario = usuarioOpt.get();

        if (!encoder.matches(req.senha, usuario.getSenhaHash())) {
            return ResponseEntity
                    .status(401)
                    .body("Email ou senha incorretos. Esqueceu a senha?");
        }
        tokenRepo.deleteByUsuario(usuario);
        // gera token simples
        String token = java.util.UUID.randomUUID().toString();
        String token_hash=encoder.encode(token);
        tokenRepo.save(new AuthToken(usuario, token_hash));

        return ResponseEntity.ok(token);
    }


    @Transactional
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestBody AuthTokenRequest req
    ) {

        var usuario=usuarioRepo.findByEmail(req.email);
        var tokenOpt = tokenRepo.findByUsuario(usuario);

        if (tokenOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Requisição Invalida");
        }
        if (!encoder.matches(req.token,tokenOpt.get().getTokenHash())) {
            return ResponseEntity
                    .status(401)
                    .body("Requisição Invalida");
        }
        tokenRepo.deleteByUsuario(usuario.get());

        return ResponseEntity.ok("Logout realizado com sucesso");
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

        var usuario=usuarioRepo.findByEmail(email);
        var tokenOpt = tokenRepo.findByUsuario(usuario);
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Requisição Invalida 1");
        }
        if(!usuario.get().getEmail().equals(email)) {
            return ResponseEntity.status(401).body("Requisição Invalida 2");
        }
        if (!encoder.matches(token,tokenOpt.get().getTokenHash())) {
            return ResponseEntity
                    .status(401)
                    .body("Requisição Inválida 3");
        }

        LocalDateTime data=tokenOpt.get().getLocalDateTime();
        LocalDateTime agora = LocalDateTime.now();

        Duration duracao = Duration.between(data, agora);

        long segundos = duracao.getSeconds();

        if (segundos > 60) {
            tokenRepo.deleteByUsuario(usuario.get());
            return ResponseEntity.status(401).body("Requisicao expirada, faça login novamente!");
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

        return ResponseEntity.ok(response);





    }
}
class PasswordResetRequest {
    public String email;
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
            @RequestBody PasswordResetRequest req
    ) {

        // 1️⃣ verifica se usuário existe
        Usuario usuario = usuarioRepo.findByEmail(req.email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Usuário não encontrado"
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

        // token técnico (UUID)
        String token = UUID.randomUUID().toString();

        tokenRepo.save(new NewPasswordToken(usuario, codigo, token));

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
            throw new RuntimeException("Erro ao enviar e-mail", e);
        }

        // 5️⃣ resposta segura
        return ResponseEntity.ok(
                "Código enviado para o seu e-mail."
        );
    }
    @Transactional
    @PostMapping("/novasenha/token")
    public ResponseEntity<?> solicitarNovaSenha(
            @RequestBody NovaSenhaTokenRequest req
    ) {

        // 1️⃣ verifica se usuário existe
        Usuario usuario = usuarioRepo.findByEmail(req.email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Usuário não encontrado"
                        )
                );


        if (!tokenRepo.existsByUsuario(usuario)) {
            return ResponseEntity.status(401).body("Código inválido");
        }

        var tokenOpt = tokenRepo.findByUsuario(usuario);
        LocalDateTime data=tokenOpt.get().getLocalDateTime();
        LocalDateTime agora = LocalDateTime.now();

        Duration duracao = Duration.between(data, agora);

        long segundos = duracao.getSeconds();

        if (segundos > 60) {
            System.out.println("segundos: " + segundos);
            tokenRepo.deleteByUsuario(usuario);
            return ResponseEntity.status(401).body("Codigo expirado, solicite novo codigo!");
        }
        String codigoCorreto=tokenOpt.get().getCodigo();
        String token = tokenOpt.get().getToken();
        if(codigoCorreto.equals(req.codigo)) {
            // 5️⃣ resposta segura
            return ResponseEntity.ok(
                    "TOKEN: " + token
            );
        }
        else{
            return ResponseEntity.status(401).body("Código inválido");
        }
    }
    @Transactional
    @PostMapping("/novasenha/cadastro")
    public ResponseEntity<?> solicitarNovaSenha(
            @RequestBody NovaSenhaCadastro req
    ) {
        if (!tokenRepo.existsByToken(req.token)) {
            return ResponseEntity.status(401).body("Nao Autorizado");
        }
        if(!req.senha.equals(req.confirmarSenha)) {
            return ResponseEntity.status(401).body("Senhas não sãos iguais");
        }
        var tokenOpt = tokenRepo.findByToken(req.token);

        Usuario usuario=tokenOpt.get().getUsuario();
        LocalDateTime data=tokenOpt.get().getLocalDateTime();
        LocalDateTime agora = LocalDateTime.now();

        Duration duracao = Duration.between(data, agora);

        long segundos = duracao.getSeconds();

        if (segundos > 60) {
            System.out.println("segundos: " + segundos);
            tokenRepo.deleteByUsuario(usuario);
            return ResponseEntity.status(401).body("Requisição expirada, solicite novo codigo!");
        }
        usuario.setSenhaHash(encoder.encode(req.senha));
        usuarioRepo.save(usuario);
        // 5️⃣ resposta segura
        return ResponseEntity.ok(
                "Nova senha configurada com sucesso!."
        );

    }
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



