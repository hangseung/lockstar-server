package com.app.lockstar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(path="/file")
public class FileController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private S3Service s3Service;

    @PostMapping
    @ResponseBody
    public ResponseEntity uploadFile (@RequestParam("username") String username, @RequestParam String password, @RequestParam("file") MultipartFile file, @RequestParam("file_key") MultipartFile fileKey) {
        User user;
        try {
            user = userService.signIn(username, password);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        try {
            s3Service.upload(file);
            s3Service.upload(fileKey);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        File newFile = new File();
        newFile.setName(file.getOriginalFilename());
        newFile.setKey(fileKey.getOriginalFilename());
        newFile.setOwnerUserId(user.getId());
        fileRepository.save(newFile);
        user.addFile(newFile);
        userRepository.save(user);

        return new ResponseEntity(newFile.getId(), HttpStatus.ACCEPTED);
    }

    @PostMapping("/{fileId}")
    @ResponseBody
    public ResponseEntity replaceFile (@PathVariable Integer fileId, @RequestParam("username") String username, @RequestParam("password") String password, @RequestParam("file") MultipartFile file) {
        User user;
        try {
            user = userService.signIn(username, password);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        if (!user.hasFilePermission(fileId)) {
            return new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);
        }

        Optional<File> originalFile = this.fileRepository.findById(fileId);
        if (originalFile.isEmpty()) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
        File editedFile = originalFile.get();

        try {
            s3Service.upload(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        editedFile.setName(file.getOriginalFilename());
        fileRepository.save(editedFile);

        return new ResponseEntity(HttpStatus.ACCEPTED);
    }

    @GetMapping("/{fileId}")
    @ResponseBody
    public ResponseEntity downloadFile (@PathVariable Integer fileId, @RequestParam("username") String username, @RequestParam("password") String password) throws NoSuchAlgorithmException {
        User user;
        try {
            user = userService.signIn(username, password);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        for (File file : user.getFile()) {
            if (file.getId().equals(fileId)) {
                try {
                    Resource resource = s3Service.download(file.getName()); // 파일을 s3에서 가져온다.
                    Resource fileKey = s3Service.download(file.getKey()); // [KSY] 파일을 암호화한 키를 가져온다.
                    return new ResponseEntity(resource, HttpStatus.ACCEPTED);
                } catch (IOException e) {
                    e.printStackTrace();
                    return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        }

        // [KSY] 파일을 암호화한 키인 fileKey 를 사용자의 public key로 암호화한다.
        // https://offbyone.tistory.com/346 를 참고함
        // 1024비트 RSA 키 쌍을 생성한다.
        SecureRandom secureRandom = new SecureRandom(); //키는 SecureRandom 클래스를 사용해서 임의의 키를 생성
        KeyPairGenerator gen; //KeyPairGenerator (String  algorithm).  지정된 알고리즘에 대한 KeyPairGenerator를 작성합니다.
        gen = KeyPairGenerator.getInstance("RSA"); // getInstance (String  algorithm)  지정된 다이제스트 알고리즘을 구현하는 KeyPairGenerator 객체를 작성합니다.

        gen.initialize(1024, secureRandom); //initialize (int keysize, SecureRandom  random) 임의의 키 사이즈 대하는 키 페어 제네레이터를 초기화합니다.
        KeyPair keyPair = gen.genKeyPair(); // 키 페어를 생성합니다.

        /**
         * Public Key로 RSA 암호화를 수행합니다.
         * @param plainText 암호화할 평문입니다.
         * @param publicKey 공개키 입니다.
         * @return
         */

        return new ResponseEntity(HttpStatus.NOT_ACCEPTABLE);
    }

    @PostMapping("/allow/{fileId}")
    @ResponseBody
    public ResponseEntity allowUsers (@PathVariable Integer fileId, @RequestParam("username") String username, @RequestParam("password") String password, @RequestParam("usernames") String allowingUsernames) {
        User user;
        try {
            user = userService.signIn(username, password);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        Optional<File> foundFile = fileRepository.findById(fileId);
        if (foundFile.isEmpty()) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
        File file = foundFile.get();

        if (!file.getOwnerUserId().equals(user.getId())) {
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }

        List<User> allowingUsers = userRepository.findByNameIn(Arrays.asList(allowingUsernames.split(",")));
        for (User allowingUser : allowingUsers) {
            allowingUser.addFile(file);
            userRepository.save(allowingUser);
        }

        return new ResponseEntity(HttpStatus.ACCEPTED);
    }
}