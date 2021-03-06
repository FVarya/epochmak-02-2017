package sample;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;import javax.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;



import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Created by Fedorova on 20/02/2017.
 */
@RestController
public class UserController {

    @NotNull
    @Autowired
    private final AccountService accountService;
    private Map<Long, String> userIdToUserCookie = new HashMap<>();


    @RequestMapping(path = "api/registration", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> Registration(@RequestBody GetBody body, HttpSession httpSession)  {
        final String login = body.getLogin();
        final String password = body.getPassword();
        final String mail = body.getMail();

        if(accountService.getUserByLogin(login)  != null)
            return  ResponseEntity.status(HttpStatus.CONFLICT).body("{\"error\": \"Login already exist\" }");
        if(accountService.getUserByMail(mail) != null)
            return  ResponseEntity.status(HttpStatus.CONFLICT).body("{\"Email\": \"already exist\" }");
        UserProfile currentUser = accountService.register(mail, login, passwordEncoder().encode(password));
        userIdToUserCookie.put(currentUser.getId(), httpSession.getId());
        httpSession.setAttribute("Login", login);
        return ResponseEntity.ok("{\"OK\": \"OK\"}");
    }

    @RequestMapping(path = "api/login", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> Login(@RequestBody GetBody body, HttpSession httpSession)  {
        final String login = body.getLogin();
        final String password = body.getPassword();
        UserProfile currentUser = accountService.getUserByLogin(login);

        if(currentUser  == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\": \"Wrong login or password\"}");
        if(passwordEncoder().matches(password, currentUser.getPassword())
                & userIdToUserCookie.get(currentUser.getId()) == httpSession.getId()){
            httpSession.setAttribute("Login", login);
            return ResponseEntity.ok("{\"OK\": \"OK\"}");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\":\"Wrong login or password\"}");
    }

    @RequestMapping(path = "api/user", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> getUser (HttpSession httpSession){
        String login = (String) httpSession.getAttribute("Login");
        if( login == null)
            return ResponseEntity.status(HttpStatus.OK).body("{\"Login\": \"User not found\"}");
        return ResponseEntity.status(HttpStatus.OK).body("{\"Login\": \"" + login + "\"}");
    }

    @RequestMapping(path = "api/logout", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> logout( HttpSession httpSession)  {
        if(httpSession.getAttribute("Login") == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\": \"User is not authorized\"}");
        httpSession.removeAttribute("Login");
        return ResponseEntity.ok("{\"OK\": \"OK\"}");
    }

    @RequestMapping(path = "api/settings", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> editUser(@RequestBody GetBodySettings body,  HttpSession httpSession)  {
        String login = (String) httpSession.getAttribute("Login");
        UserProfile currentUser = accountService.getUserByLogin(login);
        if(currentUser == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\": \"User not found\"}");
        String type = body.getType();
        String value = body.getValue();
        if( type == null & type.equals(""))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\": \"Empty type\"}");
        if( value == null & value.equals(""))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\": \"Empty value\"}");
        accountService.changeUser(currentUser, type, value);
        httpSession.setAttribute("Login", currentUser.getLogin());
        return ResponseEntity.ok("{\"OK\": \"OK\"}");
    }

    @RequestMapping(path = "api/setscore", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> setScore(@RequestBody GetBodySettings body,  HttpSession httpSession)  {
        String login = (String) httpSession.getAttribute("Login");
        if(login == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\": \"Empty user\"}");
        UserProfile currentUser = accountService.getUserByLogin(login);
        if(currentUser == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\": \"User not found\"}");
        Integer score = body.getScore();
        if(score == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\": \"Empty score\"}");
        accountService.changeScore(currentUser, body.getScore());
        return ResponseEntity.ok("{\"OK\": \"OK\"}");

    }

    @RequestMapping(path = "api/getscore", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> getScore (HttpSession httpSession){
        String login = (String) httpSession.getAttribute("Login");
        if(login == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\": \"Empty user\"}");
        UserProfile currentUser = accountService.getUserByLogin(login);
        if(currentUser == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\": \"User not found\"}");
        return  ResponseEntity.status(HttpStatus.OK).body("{\"score\":\"" + currentUser.getScore() + "\"}");
    }

    @Autowired
    public UserController(@NotNull AccountService accountService) {
        this.accountService = accountService;
    }

    private static final class GetBody {
        private String mail, login, password;

        @JsonCreator
        @SuppressWarnings({"unused", "null"})
        GetBody(@JsonProperty("login") String login, @JsonProperty("password") String password, @JsonProperty("mail") String mail ) {
            this.login = login;
            this.mail = mail;
            this.password = password;
        }

        public String getLogin() {
            return login;
        }

        public String getPassword() {
            return password;
        }

        public String getMail() {
            return mail;
        }
    }

    private static final class GetBodySettings {
        private String  type, value;
        private Integer score;

        @JsonCreator
        @SuppressWarnings({"unused", "null"})
        GetBodySettings(@JsonProperty("type") String type, @JsonProperty("value") String value, @JsonProperty("score") Integer score) {
            this.type = type;
            this.value = value;
            this.score = score;
        }

        public String getType() {
            return type;
        }

        public String getValue() {
            return value;
        }

        public Integer getScore() {
            return score;
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
