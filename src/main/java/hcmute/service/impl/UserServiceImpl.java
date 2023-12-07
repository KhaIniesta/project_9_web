package hcmute.service.impl;

import java.util.List;
import java.util.Optional;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import hcmute.entity.RoleEntity;
import hcmute.entity.UserEntity;
import hcmute.entity.UserRoleEntity;
import hcmute.model.AuthProvider;
import hcmute.repository.RoleRepository;
import hcmute.repository.UserRepository;
import hcmute.repository.UserRoleRepository;
import hcmute.service.IUserService;
import net.bytebuddy.utility.RandomString;

@Service
@Transactional
public class UserServiceImpl implements IUserService {

    @Autowired
    UserRepository userRepo;

    @Autowired
    RoleRepository roleRepo;

    @Autowired
    UserRoleRepository userRoleRepo;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JavaMailSender javaMailSender;

    @Override
    public Optional<UserEntity> findById(Integer id) {
        return userRepo.findById(id);
    }

    @Override
    public Optional<UserEntity> findByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    @Override
    public Optional<UserEntity> findByEmail(String email) {
        return userRepo.findByEmail(email);
    }

    @Override
    public List<UserEntity> getAdministators() {
        return userRepo.getAdministrators();
    }

    @Override
    public List<UserEntity> findAll() {
        return userRepo.findAll();
    }

    @Override
    public void processOAuthPostLogin(String username, String email, String image, String oauth2ClientName) {
        Optional<UserEntity> existAcc = userRepo.findByEmail(email);
        if (!existAcc.isPresent()) {
            UserEntity newAcc = new UserEntity();
            AuthProvider authProvider = AuthProvider.valueOf(oauth2ClientName.toUpperCase());
            newAcc.setUsername(username);
            newAcc.setEmail(email);
            newAcc.setProvider(authProvider);
            newAcc.setImage_url(image);
            newAcc.setEnabled(true);
            
            System.out.println(newAcc.toString());
            userRepo.save(newAcc);
        }
    }

    @Override
    public void updateAuthenticationTypeOAuth(String username, String oauth2ClientName) {
        AuthProvider authProvider = AuthProvider.valueOf(oauth2ClientName.toUpperCase());
        userRepo.updateAuthenticationTypeOAuth(username, authProvider);
    }

    @Override
    public void updateAuthenticationTypeDB(String username, String oauth2ClientName) {
        AuthProvider authProvider = AuthProvider.valueOf(oauth2ClientName.toUpperCase());
        userRepo.updateAuthenticationTypeDB(username, authProvider);
    }

    @Override
    public void register(UserEntity user, String url) throws MessagingException {
        // save user
        String encodedPassword = encoder.encode(user.getPassword());
        String randomCode = RandomString.make(64);
        user.setPassword(encodedPassword);
        user.setVerify_code(randomCode);
        user.setEnabled(false);
        user.setProvider(AuthProvider.DATABASE);
        UserEntity savedUser = userRepo.save(user);
        Optional<RoleEntity> role = roleRepo.findById("USER");
        userRoleRepo.save(new UserRoleEntity(savedUser, role.get()));
        sendVerifyEmail(savedUser, url);

    }

    @Override
    public void sendVerifyEmail(UserEntity user, String url) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");

        try {
            helper.setTo(user.getEmail());
            helper.setSubject("MilkTea - Verify your email");

            String content = "Thân gửi " + user.getUsername() + ",<br>"
                    + "Vui lòng nhấp vào đường dẫn bên dưới để xác nhận việc đăng ký tài khoản:<br>"
                    + "<h3><a href=\"" + url + "/security/verify?code=" + user.getVerify_code() + "\" target=\"_self\">VERIFY</a></h3>"
                    + "Trân trọng!<br>";

            helper.setText(content, true); 

            javaMailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public UserEntity save(UserEntity user) {
        String encodedPassword = encoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        return userRepo.save(user);
    }

    @Override
    public UserEntity update(UserEntity user) {
        Optional<UserEntity> findUser = userRepo.findByUsername(user.getUsername());
        if (findUser.isPresent()) {
            if (encoder.matches(user.getPassword(), findUser.get().getPassword())) {
                user.setPassword(encoder.encode(user.getPassword()));
            } else if (!encoder.matches(user.getPassword(), findUser.get().getPassword())) {
                user.setPassword(encoder.encode(user.getPassword()));
            } else {

            }
        }
        return userRepo.save(user);
    }

    @Override
    public void deleteByUsername(String username) {
        userRepo.deleteByUsername(username);
    }

    @Override
    public boolean verify(String verifyCode) {
        UserEntity user = userRepo.findByVerifyCode(verifyCode);
        if (user == null || user.getEnabled()) {
            return false;
        } else {
            user.setVerify_code("0");
            user.setEnabled(true);
            userRepo.save(user);
            return true;
        }
    }

}
