# legal-i IAM Integration

## SSO Overview
- User login to legal-i through single sign-on on the customer's IDP.
- All common SSO protocols are supported, legal-i handles auth with Auth0.
- Authorization on Workspaces is defined with groups in the customer's IDP.
- Authorization on legal cases is defined with access groups. Access grups are mapped from the customer's IDP.

### Login
- The users login on `https://{customer-prefix}.app.legal-i.ch` and are redirected to the customer's login page.
- Users that are authorized for multiple legal-i workspaces (e.g. `test`, `int`, `prod`) can choose the desired workspace after login

### Requirements (Example Azure AD)
Single-Sign-On-Application defined with a callback to:
```
https://auth.legal-i.ch/login/callback
```

The SSO app requires the following delegated permissions to read the user profiles:
```
Users > User.Read
Directory > Directory.Read.All
```
Refer to Auth0’s guide for further details or other SSO integrations.
```
https://auth0.com/docs/authenticate/identity-providers/enterprise-identity-providers/azure-active-dir
ectory/v2
```

If a customer has multiple workspaces, e.g. for `test`, `int`, `prod`, the same SSO application is used.

## Authorization on Workspaces
- Users are authorized by their AD groups.
- The customer defines the mapping between AD groups, legal-i workspaces, and roles.
- Examples of such mappings are:
	- If the user has the group `any-prefix-legali-produktion-sachbearbeiter`, he will be authorized to the `prod` workspace and be given `basic` role.
	- If the user has the group `any-prefix-legali-development-admin`, he will be authorized to the `dev` workspace and be given `workspace admin` role.


## Roles in legal-i

**Workspace Admins** have access to...
- all legal cases in a workspace
- admin functions like audit logs, workspace settings or reporting
- can CRUD legal cases and source files

This role is given to the project lead.

**Power Users** have access to
- all legal cases in the workspace
- can crud legal cases and source file

This role is given to internal power user to support basic users.

**Basic Users** have strictly limited access
- cannot CRUD legal cases or source files
- ca only access to legal cases to which they are authorized by access groups

## Authorization on Legal Cases with Access Groups
Legal cases within one workspace are segregated using access group. Access groups are mapped from the AD groups to the users during SSO. Example:
- The mapping prefix is configured as `any-prefix-legali-`. All groups that contain this prefix are mapped to the legal-i user as access groups.

Given a legal case with a defined access group, basic users can access this legal case if, and only if, they possess the same access group themselves.

*Example*

Given the following users are authorized on a workspace:
- Markus is a Basic User with access groups ["liability"]
- Achim is a Basic User with access groups ["accident"]
- Nicolas is a Basic User with access groups ["accident", "liability"]
- Ralph is a Power User.

then...

- Markus can access all the legal cases with the group "liability". He cannot access any legal cases with group "accident" or any other group. For Achim, it's the same, but he is restricted to legal cases from "accident".
- Nicolas has access to legal cases that contain either the group "accident" or "liability".
- Ralph can access all legal cases, since he has the power user role. Further, only Ralph can access a legal case tagged with "other-department".
