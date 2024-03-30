import React, { useState, useEffect } from 'react';
import loadWebAuthn from './loadWebauthnLibrary';

import WebAuthn from './WebAuthn';
 function Login({onLoginSuccess}) {
    let basePath = window?.APP_CONFIG?.API_BASE_URL
    const calculateApiBaseUrl = window?.APP_CONFIG?.CALCULATE_API_BASE_URL

    if (calculateApiBaseUrl) {
    // If calculateApiBaseUrl then just replace "ui-super-heroes" with "rest-fights" in the current URL
    basePath = window.location.protocol + "//" + window.location.host.replace('ui-super-heroes', 'rest-fights')
    
    }
     const [userName, setUserName] = useState('');
     const [firstName, setFirstName] = useState('');
     const [lastName, setLastName] = useState('');
     const [userPlan, setUserPlan] = useState('');
     const [result, setResult] = useState('');
     
    
     useEffect(() => {
         fetch('/api/public/me')
             .then(response => response.text())
             .then(name => setResult(`User: ${name}`));
     }, []);

     const handleLogin = () => {
        setResult('');
        const webAuthn = new WebAuthn({
            callbackPath: `${basePath}` +'/q/webauthn/callback',
            registerPath: `${basePath}` +'/q/webauthn/register',
            loginPath: `${basePath}` +'/q/webauthn/login'
        })
        webAuthn.login({ name: userName })
                .then(body => {
                    setResult(`User: ${userName}`);
                    onLoginSuccess();
                })
                .catch(err => {
                    setResult(`Login failed: ${err}`);
                });
    };

     const handleRegister = () => {
        setResult('');
        const webAuthn = new WebAuthn({
            callbackPath: `${basePath}` +'/q/webauthn/callback',
            registerPath: `${basePath}` +'/q/webauthn/register',
            loginPath: `${basePath}` +'/q/webauthn/login'
      })
      webAuthn.registerOnly({
        name: userName, 
        plan: userPlan, 
        displayName: firstName + " " + lastName
    })
    .then(body => {
        console.log(body)
        let formData = new FormData();
        formData.append('userName', userName);
        formData.append('plan', userPlan);
        formData.append('displayName', firstName + " " + lastName);
        // Assuming body contains id, rawId, response, and type based on your script.
        formData.append('webAuthnId', body.id);
        formData.append('webAuthnRawId', body.rawId);
        formData.append('webAuthnResponseAttestationObject', body.response.attestationObject);
        formData.append('webAuthnResponseClientDataJSON', body.response.clientDataJSON);
        formData.append('webAuthnType', body.type);
        return fetch( `${basePath}` + '/register', {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Accept': 'application/json'
        },
            body: formData
        });
            })
            .then(res => {
                if (res.status >= 200 && res.status < 300) {
                    return res.text();
                }
                throw new Error(res.statusText);
            })
            .then(message => setResult(message))
            .catch(error => setResult(`Registration failed: ${error}`));
                  
    };

    return (
         <div>
             <nav>
                 <ul>
                     <li><a href="/api/public">Public API</a></li>
                     <li><a href="/api/users/me">User API</a></li>
                     <li><a href="/api/admin">Admin API</a></li>
                     <li><a href= {`${basePath}/q/webauthn/logout`}>Logout</a></li>
                 </ul>
             </nav>
             <div className="container">
                 <div className="item">
                     <h1>Status</h1>
                     <div id="result">{result}</div>
                 </div>
                 <div className="item">
                     <h1>Login</h1>
                     <p>
                         <input id="userNameLogin" placeholder="User name" value={userName} onChange={(e) => setUserName(e.target.value)} /><br />
                         <button id="login" onClick={handleLogin}>Login</button>
                     </p>
                 </div>
                 <div className="item">
                     <h1>Register</h1>
                     <p>
                         <input id="userNameRegister" placeholder="User name" value={userName} onChange={(e) => setUserName(e.target.value)} /><br />
                         <input id="firstName" placeholder="First name" value={firstName} onChange={(e) => setFirstName(e.target.value)} /><br />
                         <input id="lastName" placeholder="Last name" value={lastName} onChange={(e) => setLastName(e.target.value)} /><br />
                         <select id="userPlan" value={userPlan} onChange={(e) => setUserPlan(e.target.value)}>
                            <option value="">--Please choose your plan--</option>
                             <option value="full">Full</option>
                             <option value="ref">Referee</option>
                             <option value="viewer">Viewer</option>
                         </select><br />
                         <button id="register" onClick={handleRegister}>Register</button>
                     </p>
                 </div>
             </div>
         </div>
     );
}


 export default Login