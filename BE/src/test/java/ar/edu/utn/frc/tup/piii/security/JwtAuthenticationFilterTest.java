package ar.edu.utn.frc.tup.piii.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtUtil jwtUtil;
    private UserDetailsService userDetailsService;
    private JwtAuthenticationFilter filter;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtUtil = mock(JwtUtil.class);
        userDetailsService = mock(UserDetailsService.class);
        filter = new JwtAuthenticationFilter(jwtUtil, userDetailsService);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSkipFilterWithoutHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldSkipFilterWithNonBearerHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic foo");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldAuthenticateValidToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer my-token");
        when(jwtUtil.getUsernameFromToken("my-token")).thenReturn("lucas");
        
        UserDetails userDetails = new User("lucas", "password", Collections.emptyList());
        when(userDetailsService.loadUserByUsername("lucas")).thenReturn(userDetails);
        when(jwtUtil.validateToken("my-token", userDetails)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("lucas");
    }

    @Test
    void shouldNotAuthenticateInvalidToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer my-token");
        when(jwtUtil.getUsernameFromToken("my-token")).thenReturn("lucas");
        
        UserDetails userDetails = new User("lucas", "password", Collections.emptyList());
        when(userDetailsService.loadUserByUsername("lucas")).thenReturn(userDetails);
        when(jwtUtil.validateToken("my-token", userDetails)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldHandleException() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer my-token");
        when(jwtUtil.getUsernameFromToken("my-token")).thenThrow(new RuntimeException("Bad JWT"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
