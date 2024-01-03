import React from 'react';
import ReactDOM from 'react-dom/client';
import './styles.css';
import App from './app/App';
import {usePromiseTracker} from "react-promise-tracker";
import logo from "./images/quarkus_icon.png";

const root = ReactDOM.createRoot(document.getElementById('root'));

const LoadingIndicator = (props) => {
  const { promiseInProgress } = usePromiseTracker({ delay: 500 });

  return (
    promiseInProgress && (
        <div className="loading-indicator">
          <img className="spin-logo" src={logo} alt="Spinning logo"/>
        </div>
    )
  );
}

root.render(
  <React.StrictMode>
    <App />
    <LoadingIndicator/>
  </React.StrictMode>
);
