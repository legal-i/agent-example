# legal-i IAM Integration

## IAM Integration
- Users are included with single sign-on on the customer's IDP.
- The customer can define the mapping between the roles or groups defined in their IDP and the role in legal-i.

### IDP Integration - Azure AD Configuration
- The users will always log in on `https://app.legal-i.ch`
- In the legal-i application, they will be able to choose the correct workspace based on their assigned
roles.
- legal-i will configure their IDP (Auth0) to integrate with the customers Azure AD.

#### Requirements
Single Sign on Application defined with a callback to:
```
https://auth.legal-i.ch/login/callback
```

Required delegated permissions to read the user profiles:
```
Users > User.Read
Directory > Directory.Read.All
```
Refer to Auth0’s guide for further details.
```
https://auth0.com/docs/authenticate/identity-providers/enterprise-identity-providers/azure-active-dir
ectory/v2
```

### User Roles and Case Authorization
- Every user needs to have at least one valid legal-i role to access legal-i.
- The mapping of the internal roles and groups can be defined by the customer.
- The roles or groups need to contain the string `*legali*`, case-insensitive.


The following roles are defined in legal-i:

**Workspace Admin**
- has access to...
- all legalcases
- admin functions, audit logs and agent settings
- can CRUD legalcases and sourcefiles

**Power User**

- has access to all legalcases in the workspace
- can crud legalcases and sourcefile

**Basic User**

- every basic user needs to have one or more access groups
- access is limited to legalcases that are have one of his groups specified

### Access Groups
All roles or groups that contain the string `*legali*` are treated as access groups.
access groups are used to model departments in the same organization.  If one of the access groups of a user matches the access group assigned to the legalcase, he is authorized to access it.

*Example*

```
User Markus is a Basic User with group ["legali-liability"]
User Achim is a Basic User with group ["legali-accident"]
User Nicolas is a Basic User with groups ["legali-accident", "legali-liability"]
User Ralph is a Power User.

Markus can access all the legalcases that have the group "legali-liability". He cannot access any legalcases with group "legali-accident" or other group. For Achim its the same, but for "legali-accident".

Nicolas, however, has access to legalcases that are contain either the group "legali-accident" or "legali-liability".
Ralph can access all legalcases, since he has the power user role.

Only Ralph can access a legalcase, that is tagged with "legali-other-department".
```


