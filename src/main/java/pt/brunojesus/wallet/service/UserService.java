package pt.brunojesus.wallet.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.brunojesus.wallet.dto.UserRegistrationDTO;
import pt.brunojesus.wallet.entity.User;
import pt.brunojesus.wallet.exception.UserAlreadyExistsException;
import pt.brunojesus.wallet.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User registerUser(UserRegistrationDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + dto.getEmail() + " already exists");
        }

        User user = new User();
        user.setEmail(dto.getEmail());

        return userRepository.save(user);
    }
}