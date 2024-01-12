package com.johann.dataaccess;


import com.johann.models.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import java.nio.file.AccessDeniedException;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class Jwt {
	
	// Secret Key
		private static String secret = "This_is_Johann";
		
		// Expiration Time
		private static long expiryDuration = 60 * 60;

		// Generate Token : header.payload.signature
		public String generateToken(User user) {
			
			long milliTime = System.currentTimeMillis();
			long expiryTime = milliTime + expiryDuration * 1000;
			
			//Set IssuedTime and ExpiryTime
			Date issuedAt = new Date(milliTime);
			System.out.println(issuedAt);
			Date expiryAt = new Date(expiryTime);
			System.out.println(expiryAt);
			
			//Claims
			Map<String, Object> claims = new HashMap<>();
			claims.put("id", user.getId());
	        claims.put("firstName", user.getFirstName());
	        claims.put("lastName", user.getLastName());
	        claims.put("mobile", user.getMobile());
	        claims.put("email", user.getEmail());
	        claims.put("blocked", user.isBlocked().toString());
	        claims.put("active", user.isActive().toString());
	        claims.put("createdAt", user.getCreatedOn());
	        claims.put("userType", user.getUserType().toString());
			
			//Claims claims= Jwts.claims().setIssuer(user.getId().toString())
			//		.setIssuedAt(issuedAt)
			//		.setExpiration(expiryAt);
		
			//generate jwt using claims
			return Jwts.builder().setClaims(claims)
					.setIssuer(user.getId().toString())
					.setIssuedAt(issuedAt)
					.setExpiration(expiryAt)
	                .signWith(SignatureAlgorithm.HS512,secret)
	                .compact();
			
		}
			
		// AccessDenied for Claims -- User-Define Exception
		public Claims verify(String authorization) throws Exception {
			
			// Authorization: 	eyJhbGciOiJIUzUxMiJ9
						//	 	.eyJpc3MiOiI3IiwiaWF0IjoxNjcxMDE2NzM5LCJleHAiOjE2NzEwMjAzMzksInVzZXJuYW1lIjoiQW51IiwiZW1haWxJZCI6ImFudUBnbWFpbC5jb20ifQ
						//		.34wBjIVRzuu4_bTWUgnpac12zUHMJoOSPXfgptNeaarxuBNlf57VhLAqsgLvlxUcbEB4YuN0Hw5lo4bRC2_3VA
			try {
				
				Claims claims= Jwts.parser().setSigningKey(secret)
						.parseClaimsJws(authorization).getBody();
				return claims;
				
			}catch (Exception e) {
				throw new AccessDeniedException("Sorry! Access Denied");
			}
		
		}
}
