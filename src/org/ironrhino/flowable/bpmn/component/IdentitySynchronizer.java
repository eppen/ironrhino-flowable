package org.ironrhino.flowable.bpmn.component;

import java.util.Objects;

import org.apache.ibatis.exceptions.PersistenceException;
import org.flowable.engine.IdentityService;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.ironrhino.core.event.EntityOperationEvent;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
public class IdentitySynchronizer {

	@Autowired
	private IdentityService identityService;

	@Autowired
	private UserDetailsService userDetailsService;

	@EventListener
	public void onApplicationEvent(EntityOperationEvent<? extends UserDetails> event) {
		if (!event.isLocal())
			return;
		UserDetails user = event.getEntity();
		user = userDetailsService.loadUserByUsername(user.getUsername());
		switch (event.getType()) {
		case CREATE:
		case UPDATE:
			BeanWrapperImpl bw = new BeanWrapperImpl(user);
			String email = null;
			if (bw.isReadableProperty("email"))
				email = (String) bw.getPropertyValue("email");
			User u = identityService.createUserQuery().userId(user.getUsername()).singleResult();
			if (u == null) {
				u = identityService.newUser(user.getUsername());
				u.setEmail(email);
				identityService.saveUser(u);
			} else {
				if (!Objects.equals(u.getEmail(), email)) {
					u.setEmail(email);
					identityService.saveUser(u);
				}
			}
			for (Group group : identityService.createGroupQuery().groupMember(u.getId()).list())
				identityService.deleteMembership(u.getId(), group.getId());
			for (GrantedAuthority ga : user.getAuthorities()) {
				Group g = identityService.createGroupQuery().groupId(ga.getAuthority()).singleResult();
				if (g == null) {
					g = identityService.newGroup(ga.getAuthority());
					try {
						identityService.saveGroup(g);
					} catch (PersistenceException e) {
						// retry for concurrent insertion
						g = identityService.createGroupQuery().groupId(ga.getAuthority()).singleResult();
						if (g == null) {
							g = identityService.newGroup(ga.getAuthority());
							identityService.saveGroup(g);
						}
					}
				}
				identityService.createMembership(u.getId(), g.getId());
			}
			break;
		case DELETE:
			identityService.deleteUser(user.getUsername());
			break;
		default:
			break;
		}
	}

}
