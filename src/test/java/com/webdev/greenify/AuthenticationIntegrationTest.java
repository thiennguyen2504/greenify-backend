package com.webdev.greenify;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthenticationIntegrationTest {

//        @Autowired
//        private MockMvc mockMvc;
//
//        @Autowired
//        private ObjectMapper objectMapper;
//
//        @MockBean
//        private EmailService emailService;
//
//        @Autowired
//        private com.webdev.greenify.repository.UserRepository userRepository;
//
//        @Autowired
//        private com.webdev.greenify.repository.RoleRepository roleRepository;
//
//        private RegisterRequest registerRequest;
//        private AuthenticationRequest authRequest;
//
//        @BeforeEach
//        public void setUp() {
//                registerRequest = RegisterRequest.builder()
//                                .firstname("Peter")
//                                .lastname("Parker")
//                                .email("peter.parker@example.com")
//                                .password("password123")
//                                .roles(Set.of("ADMIN"))
//                                .build();
//
//                authRequest = AuthenticationRequest.builder()
//                                .email("peter.parker@example.com")
//                                .password("password123")
//                                .build();
//        }
//
//        private void enableUser() {
//                var user = userRepository.findByEmail("peter.parker@example.com").orElseThrow();
//                userRepository.save(user);
//        }
//
//        @Test
//        public void shouldRegisterUserSuccessfully() throws Exception {
//                mockMvc.perform(post("/api/v1/auth/register")
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .content(objectMapper.writeValueAsString(registerRequest)))
//                                .andDo(MockMvcResultHandlers.print())
//                                .andExpect(status().isOk());
//        }
//
//        @Test
//        public void shouldAuthenticateUserAndReturnToken() throws Exception {
//                // Register first
//                mockMvc.perform(post("/api/v1/auth/register")
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .content(objectMapper.writeValueAsString(registerRequest)))
//                                .andDo(MockMvcResultHandlers.print());
//
//                enableUser();
//
//                // Authenticate
//                MvcResult result = mockMvc.perform(post("/api/v1/auth/authenticate")
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .content(objectMapper.writeValueAsString(authRequest)))
//                                .andDo(MockMvcResultHandlers.print())
//                                .andExpect(status().isOk())
//                                .andExpect(jsonPath("$.access_token").exists())
//                                .andExpect(jsonPath("$.refresh_token").exists())
//                                .andReturn();
//
//                String response = result.getResponse().getContentAsString();
//                AuthenticationResponse authResponse = objectMapper.readValue(response, AuthenticationResponse.class);
//
//                // Test protected resource with token
//                mockMvc.perform(get("/api/v1/users")
//                                .header("Authorization", "Bearer " + authResponse.getAccessToken()))
//                                .andDo(MockMvcResultHandlers.print())
//                                .andExpect(status().isOk())
//                                .andExpect(jsonPath("$[0].email").value("peter.parker@example.com"));
//        }
//
//        @Test
//        public void shouldFailAuthenticationWithWrongPassword() throws Exception {
//                // Register first
//                mockMvc.perform(post("/api/v1/auth/register")
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .content(objectMapper.writeValueAsString(registerRequest)))
//                                .andDo(MockMvcResultHandlers.print());
//
//                enableUser();
//
//                AuthenticationRequest wrongAuthRequest = AuthenticationRequest.builder()
//                                .email("peter.parker@example.com")
//                                .password("wrongpassword")
//                                .build();
//
//                mockMvc.perform(post("/api/v1/auth/authenticate")
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .content(objectMapper.writeValueAsString(wrongAuthRequest)))
//                                .andDo(MockMvcResultHandlers.print())
//                                .andExpect(status().isUnauthorized()); // Or status 403 depending on config, but usually
//                                                                       // 401/403
//        }
//
//        @Test
//        public void shouldValidateRegisterRequest() throws Exception {
//                RegisterRequest invalidRequest = RegisterRequest.builder()
//                                .firstname("") // Invalid
//                                .lastname("Parker")
//                                .email("not-an-email") // Invalid
//                                .password("pass")
//                                .build();
//
//                mockMvc.perform(post("/api/v1/auth/register")
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .content(objectMapper.writeValueAsString(invalidRequest)))
//                                .andDo(MockMvcResultHandlers.print())
//                                .andExpect(status().isBadRequest())
//                                .andExpect(jsonPath("$.firstname").exists())
//                                .andExpect(jsonPath("$.email").exists());
//        }
}
