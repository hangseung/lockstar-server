package com.app.lockstar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.KeyPair; // [KSY]
import java.security.KeyPairGenerator; // [KSY]
import java.security.NoSuchAlgorithmException; // [KSY]
import java.security.SecureRandom; // [KSY]
import java.util.Optional;

@RestController
@RequestMapping(path="/file")
public class FileController {
    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private S3Service s3Service;

    @PostMapping
    @ResponseBody
    public ResponseEntity uploadFile (@RequestParam("file") MultipartFile file, @RequestParam("file_key") MultipartFile fileKey) {
        String uploadedFilePath, uploadedFileKeyPath;
        try {
            uploadedFilePath = s3Service.upload(file);
            uploadedFileKeyPath = s3Service.upload(fileKey);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        File newFile = new File();
        newFile.setName(file.getOriginalFilename());
        newFile.setKey(fileKey.getOriginalFilename());
        fileRepository.save(newFile);

        return new ResponseEntity(newFile.getId(), HttpStatus.ACCEPTED);
    }

    @GetMapping("/{fileId}")
    @ResponseBody
    public ResponseEntity downloadFile (@PathVariable Integer fileId, @RequestParam("username") String username, @RequestParam("password") String password) throws NoSuchAlgorithmException {
        Optional<User> sameNameUser = userRepository.findByName(username);

        if (sameNameUser.isEmpty()
        || !sameNameUser.get().isSamePassword(password)) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }

        User user = sameNameUser.get();
        for (File file : user.getFile()) {
            if (file.getId().equals(fileId)) {
                try {
                    Resource resource = s3Service.download(file.getName()); // 파일을 s3에서 가져온다.
                    Resource file_key=s3Service.download(file.getKey()); // [KSY] 파일을 암호화한 키를 가져온다.
                    return new ResponseEntity(resource, HttpStatus.ACCEPTED);
                } catch (IOException e) {
                    e.printStackTrace();
                    return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        }

        // [KSY] 파일을 암호화한 키인 file_key 를 사용자의 public key로 암호화한다.
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
}