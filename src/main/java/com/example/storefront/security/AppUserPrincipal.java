package com.example.storefront.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The authenticated caller. Produced two ways:
 *   - by {@link AppUserDetailsService} during password login (carries the hash);
 *   - by {@link JwtAuthenticationFilter} from token claims (no hash needed).
 */
public class AppUserPrincipal implements UserDetails {

    private final Long userId;
    private final String email;
    private final String passwordHash;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public AppUserPrincipal(Long userId, String email, String passwordHash,
                            boolean enabled, Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.authorities = authorities;
    }

    public static AppUserPrincipal fromEntity(AppUser user) {
        return new AppUserPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(),
                user.isEnabled(), toAuthorities(user.getRoles().stream().map(Role::authority).toList()));
    }

    public static AppUserPrincipal fromToken(Long userId, String email, List<String> authorities) {
        return new AppUserPrincipal(userId, email, null, true, toAuthorities(authorities));
    }

    private static List<SimpleGrantedAuthority> toAuthorities(List<String> names) {
        return names.stream().map(SimpleGrantedAuthority::new).toList();
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
