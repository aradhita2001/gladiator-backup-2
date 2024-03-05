package com.example.capstone.jwt;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import com.example.capstone.dto.Constants;
import com.example.capstone.entity.Account;
import com.example.capstone.entity.AccountRequest;
import com.example.capstone.entity.Loan;
import com.example.capstone.entity.User;
import com.example.capstone.repository.AccountRepository;
import com.example.capstone.repository.AccountRequestRepository;
import com.example.capstone.repository.LoanRepository;
import com.example.capstone.repository.UserRepository;

@Component
public class JwtUtil {
    public static final String SECRET = "5367566B59703373367639792F423F45284842342342324452352B4D";

    @Autowired
    private UserRepository UserRepository; 

    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private LoanRepository loanRepository; 

    @Autowired
    private AccountRequestRepository accountRequestRepository;
    
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }


    public String generateToken(String userName){
        Map<String,Object> claims=new HashMap<>();
        //TODO
        User user= UserRepository.findByEmail(userName).get();
        claims.put("userId", user.getUserId());
        claims.put("role", user.getRole());
        claims.put("userName", userName);
        return createToken(claims,userName);
    }

    private String createToken(Map<String, Object> claims, String userName) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userName)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis()+1000*60*30))
                .signWith(getSignKey(), SignatureAlgorithm.HS256).compact();
    }

    private Key getSignKey() {
        byte[] keyBytes= Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean validateUser(String authHeader, long userId){
        if (authHeader != null && authHeader.startsWith(Constants.JWT_TOKEN_PREFIX)) {
            String token = authHeader.substring(Constants.JWT_TOKEN_PREFIX.length());
            Claims claims = extractAllClaims(token);
            if(claims.get("role").equals("USER") && ((int) claims.get("userId") == userId)) return true;
        }
        return false;
    }

    public boolean validateUserByAccountId(String authHeader, Long accountId){

        Optional<Account> account = accountRepository.findById(accountId);
        if(!account.isPresent()) return false;
        long userId = account.get().getCustomer().getUserId();
        return validateUser(authHeader, userId);
    }

    public boolean validateUserByAccountRequestId(String authHeader, Long accountRequestId){

        Optional<AccountRequest> accountRequest = accountRequestRepository.findById(accountRequestId);
        if(!accountRequest.isPresent()) return false;
        long userId = accountRequest.get().getCustomer().getUserId();
        return validateUser(authHeader, userId);
    }

    public boolean validateUserByLoanId(String authHeader, Long loanId){

        Optional<Loan> loan = loanRepository.findById(loanId);
        if(!loan.isPresent()) return false;
        long userId = loan.get().getCustomer().getUserId();
        return validateUser(authHeader, userId);
    }

    public boolean validateAdmin(String authHeader){
        
        if (authHeader != null && authHeader.startsWith(Constants.JWT_TOKEN_PREFIX)) {
            String token = authHeader.substring(Constants.JWT_TOKEN_PREFIX.length());
            Claims claims = extractAllClaims(token);
            System.out.println(claims.get("role"));

            if(claims.get("role").equals("ADMIN")) return true;

        }
        
        return false;
    }
}
