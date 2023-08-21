# legal-i IAM Integration

## SSO Overview
- User login to legal-i through single sign-on on the customer's IDP.
- All common SSO protocols are supported, legal-i handles auth with Auth0.
- Authorization on Workspaces is defined with groups in the customer's IDP.
- Authorization on legal cases is defined with access groups. Access grups are mapped from the customer's IDP.

### Login
- The users login on `https://{customer-prefix}.app.legal-i.ch` and are redirected to the customer's login page.
- Users that are authorized for multiple legal-i workspaces (e.g. `test`, `int`, `prod`) can choose the desired workspace after login

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


## Configuration of Identity providers

The following section provides guidance on how to configure the customer's IDP.
An example is provided for Azure AD, but other supported IDPs (OIDC, SAML, ...) should work in a similar fashion.

### Choice of user identifier

When creating a `LegalCase` using the SDK or the API, the customer has to provide a reference to a user (i.e. a user ID) in the `owner` field.

It is important that the customer's IDP passes the same user ID during SSO login, so that the legal case can reference that user.

Thus, the customer has to decide which user ID to use for the `owner` field:
- It needs to be **unique** and **immutable**. This usually rules out email addresses, since they can change at most companies (i.e. if an employee gets married)
- When using Azure AD, the user's object ID could be used. However, most companies don't want to use that since it could change when changing IDP,
  and often the systems creating the legal cases do not have access to the internal Azure AD object ID
- So, ideally a stable internal employee ID (personal number) should be used. Be aware that Azure AD requires the employee ID be configured as a custom claim, so it's passed on SSO login to Auth0.

### Vanilla Azure AD configuration

If no custom claim configuration is required (see step above), a default Azure AD Auth0 configuration can be used.

Documentation: https://auth0.com/docs/authenticate/identity-providers/enterprise-identity-providers/azure-active-directory/v2

Enterprise Application defined with a callback to:
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
https://auth0.com/docs/authenticate/identity-providers/enterprise-identity-providers/azure-active-directory/v2
```

If a customer has multiple workspaces, e.g. for `test`, `int`, `prod`, the same SSO application is used.


### OIDC Azure AD Configuration

When employee ID - or any other custom claim - needs to be passed from Azure AD, Auth0 has to use an OpenID Connect connection
(instead of an Azure AD connection, which does not support custom claims), which requires a slightly different configuration on Azure AD side.

Create an Enterprise Application & App Registration (same as in the official guide above).

Then, the `groups` claim needs to be manually configured:
- Select the created **App registration**
- Click on **Token configuration**, **Add groups claim**
- Select **Security groups** and choose **sAMAccountName** in the Tab **Access**

#### Configure Employee ID on Azure AD

If an employee ID (or a different field) should be passed from Azure AD (see section below),
an Azure AD policy has to be configured; the following shows example powershell commands:

```powershell
# connect to AD
AzureADPreview\Connect-AzureAD

# Create Azure AD Policy with definition for employeeid
New-AzureADPolicy -Definition @('{"ClaimsMappingPolicy":{"Version":1,"IncludeBasicClaimSet":"true", "ClaimsSchema": [{"Source":"user","ID":"employeeid","SamlClaimType":http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name,"JwtClaimType":"employeeid"}]}}') -DisplayName "employeeid" -Type "ClaimsMappingPolicy"

# Add employeeid Policy to a service principal
Add-AzureADServicePrincipalPolicy -Id <ObjectId from service principal> -RefObjectId <Policy Id>

# as a last thing, make sure acceptMappedClaims is set to true in the App registration's manifest.
```