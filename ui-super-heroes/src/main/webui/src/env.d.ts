declare var process: {
  env: {
    // Provides auto complete when typing process.env.YOUR_VAR_NAME. These
    // are always string type, since they're read from env
    NG_APP_ENV: string;
    // Replace the line below with your environment variable for better type checking
    [key: string]: any;
  };
};
