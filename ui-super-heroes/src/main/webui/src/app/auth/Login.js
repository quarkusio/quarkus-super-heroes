import React, { useState, useEffect } from 'react';
import loadWebAuthn from './loadWebauthnLibrary';

 function Login() {
    let basePath = window?.APP_CONFIG?.API_BASE_URL
    const calculateApiBaseUrl = window?.APP_CONFIG?.CALCULATE_API_BASE_URL

    if (calculateApiBaseUrl) {
    // If calculateApiBaseUrl then just replace "ui-super-heroes" with "rest-fights" in the current URL
    basePath = window.location.protocol + "//" + window.location.host.replace('ui-super-heroes', 'rest-fights')
    
    }
  

    console.log('Login Component Rendering');
    if (window.WebAuthn)
    {
        console.log("initialize")
        const webAuthn = new window.WebAuthn.constructor({
            callbackPath: '/q/webauthn/callback',
            registerPath: '/q/webauthn/register',
            loginPath: '/q/webauthn/login'
      });
    }
     const [userName, setUserName] = useState('');
     const [firstName, setFirstName] = useState('');
     const [lastName, setLastName] = useState('');
     const [userRole, setUserRole] = useState('admin');
     const [result, setResult] = useState('');
     
    const [loaded, setLoaded] = useState(false);  
    useEffect(() => {
        loadWebAuthn(() => {
        setLoaded(true);
        });
    });
    
     useEffect(() => {
         fetch('/api/public/me')
             .then(response => response.text())
             .then(name => setResult(`User: ${name}`));
     }, []);
     const handleLogin = () => {
        setResult('');
        if(window.webAuthn) {
            window.webAuthn.login({ name: userName })
                .then(body => {
                    setResult(`User: ${userName}`);
                })
                .catch(err => {
                    setResult(`Login failed: ${err}`);
                });
        } else {
            console.log('webAuthn is not loaded yet');
        }
    };
     const handleRegister = () => {
        setResult('');
        if (window.webAuthn) {
            const WebAuthn = new window.WebAuthn.constructor({
                callbackPath: `${basePath}` +'/q/webauthn/callback',
                registerPath: `${basePath}` +'/q/webauthn/register',
                loginPath: `${basePath}` +'/q/webauthn/login'
          });
            console.log(WebAuthn)
            WebAuthn.registerOnly({
                name: userName, 
                role: userRole, 
                displayName: firstName + " " + lastName
            })
            .then(body => {
                console.log(body)
                let formData = new FormData();
                formData.append('userName', userName);
                formData.append('role', userRole);
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
        } else {
            console.log('webAuthn is not loaded yet');
            setResult('Registration failed: webAuthn not available');
        }
    };
    

     return (
         <div>
             <div className="maps-component">
                {loaded ? '' : ''}
            </div>
             <nav>
                 <ul>
                     <li><a href="/api/public">Public API</a></li>
                     <li><a href="/api/users/me">User API</a></li>
                     <li><a href="/api/admin">Admin API</a></li>
                     <li><a href="/q/webauthn/logout">Logout</a></li>
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
                         <select id="userRole" value={userRole} onChange={(e) => setUserRole(e.target.value)}>
                             <option value="admin">Admin</option>
                             <option value="referee">Referee</option>
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