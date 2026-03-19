package dk.ek.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.bugelhartmann.ITokenSecurity;
import dk.bugelhartmann.TokenSecurity;
import dk.bugelhartmann.UserDTO;
import dk.ek.exceptions.ApiException;
import dk.ek.exceptions.ValidationException;
import dk.ek.persistence.HibernateConfig;
import dk.ek.persistence.ISecurityDAO;
import dk.ek.persistence.SecurityDAO;
import dk.ek.persistence.User;
import dk.ek.utils.Utils;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;


public class SecurityController implements ISecurityController{
    private ISecurityDAO securityDAO = new SecurityDAO(HibernateConfig.getEntityManagerFactory());
    private ObjectMapper objectMapper = new ObjectMapper();
    private ITokenSecurity tokenSecurity = new TokenSecurity();

    @Override
    public void register(Context ctx) {
        UserDTO user = ctx.bodyAsClass(UserDTO.class);
        securityDAO.createUser(user.getUsername(), user.getPassword());
        ObjectNode node = objectMapper.createObjectNode();
        node.put("msg","login register succesfull");
        ctx.json(node).status(201);
    }

    @Override
    public void login(Context ctx) {
        UserDTO user = ctx.bodyAsClass(UserDTO.class);
        try {
            User userEntity = securityDAO.getVerifiedUser(user.getUsername(), user.getPassword());
            String token = createToken(new UserDTO(userEntity.getUserName(), userEntity.getRolesAsStrings()));
            ObjectNode node = objectMapper.createObjectNode();

            ctx.status(200).json(node
                    .put("token", token)
                    .put("username", userEntity.getUserName()));
        } catch(ValidationException ex){
            throw new ApiException(401, ex.getMessage());
        }

    }

    @Override
    public void authenticate(Context ctx) {

    }

    @Override
    public void authorize(Context ctx) {

    }
    private String createToken(UserDTO user) {
        try {
            String ISSUER;
            String TOKEN_EXPIRE_TIME;
            String SECRET_KEY;

            if (System.getenv("DEPLOYED") != null) {
                ISSUER = System.getenv("ISSUER");
                TOKEN_EXPIRE_TIME = System.getenv("TOKEN_EXPIRE_TIME");
                SECRET_KEY = System.getenv("SECRET_KEY");
            } else {
                ISSUER = Utils.getPropertyValue("ISSUER", "config.properties");
                TOKEN_EXPIRE_TIME = Utils.getPropertyValue("TOKEN_EXPIRE_TIME", "config.properties");
                SECRET_KEY = Utils.getPropertyValue("SECRET_KEY", "config.properties");
                System.out.println(ISSUER+" "+TOKEN_EXPIRE_TIME+" "+SECRET_KEY);
            }
            return tokenSecurity.createToken(user, ISSUER, TOKEN_EXPIRE_TIME, SECRET_KEY);
        } catch (Exception e) {
//            logger.error("Could not create token", e);
            e.printStackTrace();
            throw new ApiException(500, "Could not create token");
        }
    }
}
