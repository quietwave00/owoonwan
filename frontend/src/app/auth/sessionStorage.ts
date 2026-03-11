const SESSION_TOKEN_KEY = "sessionToken";
const UNAUTHORIZED_EVENT = "owoonwan:unauthorized";

export function getSessionToken() {
  return localStorage.getItem(SESSION_TOKEN_KEY);
}

export function setSessionToken(sessionToken: string) {
  localStorage.setItem(SESSION_TOKEN_KEY, sessionToken);
}

export function clearSessionToken() {
  localStorage.removeItem(SESSION_TOKEN_KEY);
}

export function dispatchUnauthorizedEvent() {
  window.dispatchEvent(new CustomEvent(UNAUTHORIZED_EVENT));
}

export function subscribeUnauthorized(handler: () => void) {
  const listener = () => handler();

  window.addEventListener(UNAUTHORIZED_EVENT, listener);

  return () => {
    window.removeEventListener(UNAUTHORIZED_EVENT, listener);
  };
}
