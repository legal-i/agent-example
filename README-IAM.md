# legal-i IAM Integration

## Single Sign-On (SSO) Overview

- Users access the legal-i platform through Single Sign-On (SSO) via their customer's Identity Provider (IDP), primarily Active Directory (AD).
- legal-i supports common SSO protocols, with authentication managed by Auth0 through Okta.
- Customer departments are mapped to different legal-i workspaces.
- Workspace authorization is configured through customer IDP groups.
- Access to legal cases within a workspace or department is controlled by access groups synchronized from the IDP.

### Login

- Users initiate the login process at `https://{workspace-prefix}.app.legal-i.ch` and are automatically redirected to the customer's login page.
- Users who have authorization for multiple workspaces, whether for staging or departments, can select their desired workspace after logging in.

## Authorization on Workspaces

- User authorization on legal-i workspaces is based on their Active Directory (AD) groups.
- The customer defines mappings between AD groups, legal-i workspaces, and associated roles.
- Examples of such mappings include:
  - Users belonging to the group `any-prefix-legali-prod-unfall-sachbearbeiter` are authorized for the `prod-unfall` workspace with the `basic` role.
  - Users in the group `any-prefix-legali-development-admin` are authorized for the `development` workspace with the `workspace admin` role.

## Roles in legal-i

**Workspace Admins** have access to:

- All legal cases within a workspace.
- Administrative functions such as audit logs, workspace settings, and reporting.
- The ability to Create, Read, Update, and Delete (CRUD) legal cases and source files.

This role is typically assigned to project leads.

**Technical Admins** have access to:

- Administrative functions, including audit logs, workspace settings, and reporting.
- However, they do not have access to the legal cases or any business data.

This role is typically assigned to internal technical staff.

**Basic Users** have strictly limited access:

- They do not have CRUD permissions for legal cases or source files.
- At the legal case level, they are authorized to see:
  - All legal cases that do not have an access group defined.
  - Legal cases with access groups if they are members of those groups.

## Authorization on Legal Cases with Access Groups

Legal cases within a workspace or department are organized using access groups, which are mapped from Active Directory (AD) groups to users during Single Sign-On (SSO). Here's how it works:

- The mapping prefix, configured as `any-prefix-legali-`, associates AD groups containing this prefix with legal-i users as access groups.

Basic users can access legal cases with access groups only if they possess the same access group themselves.

**Example:**

Consider the following legal-i team members authorized on a given workspace:

- Markus is a Basic User without any access groups.
- Nicolas is a Basic User with access groups ["vip"].
- Achim is a Basic User with access groups ["employees"].
- Aimé is a Basic User with access groups ["vip", "employees"].
- Ralph is a Workspace Admin User.
- Nikolas is a Technical Admin User.

In this scenario:

- Markus can access all legal cases that do not have an access group defined.
- Nicolas can access all legal cases that do not have an access group defined or have the access group "vip" defined.
- Achim can access all legal cases that do not have an access group defined or have the access group "employees" defined.
- Aimé can access all legal cases that do not have an access group defined or have the access group "vip" or "employees" defined.
- Ralph can access all legal cases, as he is a Workspace Admin User.
- Nikolas cannot access any legal cases, as he is a Technical Admin User.

## Configuration of Identity Providers

This section provides guidance on configuring the customer's Identity Provider (IDP) for seamless integration with legal-i. While an example is provided for Azure AD, similar configurations apply to other supported IDPs, such as OIDC and SAML.

### Choice of User Identifier

When creating a `LegalCase` using the SDK or the API, it is essential that the customer provides a reference to a user (i.e., a user ID) in the `owner` field. To ensure smooth operation, the customer's IDP must pass the same user ID during Single Sign-On (SSO) login to allow proper referencing of the legal case. Considerations include:

- The user ID should be **unique** and **immutable**, ruling out email addresses, which can change, especially in the case of name changes.
- In Azure AD, the user's object ID is a candidate for use. However, many companies prefer a more stable internal employee ID (personal number). Configuring Azure AD to pass the employee ID as a custom claim during SSO login is necessary.

### Vanilla Azure AD Configuration

If no custom claim configuration is required, a default Azure AD Auth0 configuration can be used. Key steps include:

- Configure an Enterprise Application with a callback to `https://auth.legal-i.ch/login/callback`.
- Ensure the SSO app has delegated permissions for reading user profiles, including `Users > User.Read` and `Directory > Directory.Read.All`.

For detailed steps, refer to Auth0’s documentation [here](https://auth0.com/docs/authenticate/identity-providers/enterprise-identity-providers/azure-active-directory/v2).

If a customer operates multiple workspaces, such as `test`, `int`, and `prod`, the same SSO application can be used.

### OIDC Azure AD Configuration

When custom claims, like employee ID, need to be passed from Azure AD, Auth0 should use an OpenID Connect (OIDC) connection instead of an Azure AD connection. Key steps include:

- Create an Enterprise Application and App Registration (similar to the official guide mentioned above).
- Manually configure the `groups` claim in the App Registration settings by selecting the created **App registration**, clicking on **Token configuration**, and adding the **groups** claim with **sAMAccountName** in the **Access** tab.

#### Configure Employee ID on Azure AD

For passing additional claims like employee ID from Azure AD, configuring an Azure AD policy is necessary. Here are example PowerShell commands for this:

```powershell
# Connect to Azure AD
AzureADPreview\Connect-AzureAD

# Create an Azure AD Policy with a definition for employee ID
New-AzureADPolicy -Definition @('{"ClaimsMappingPolicy":{"Version":1,"IncludeBasicClaimSet":"true", "ClaimsSchema": [{"Source":"user","ID":"employeeid","SamlClaimType":"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name","JwtClaimType":"employeeid"}]}}')
