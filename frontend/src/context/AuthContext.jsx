import {
  createContext,
  useContext,
  useEffect,
  useState,
} from "react";

import api from "../services/api";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {

  const [token, setToken] = useState(
    localStorage.getItem("stockly_token")
  );

  const [user, setUser] = useState(null);

  const [loading, setLoading] = useState(true);


  /*
   * Fetch the currently logged-in user
   */
  const fetchUser = async () => {

    try {

      const response = await api.get("/users/me");

      setUser(response.data);

    } catch (error) {

      console.error(
        "Failed to fetch authenticated user:",
        error
      );

      setUser(null);

    } finally {

      setLoading(false);

    }
  };


  /*
   * When the app starts, check
   * whether a JWT already exists.
   */
  useEffect(() => {

    if (token) {

      fetchUser();

    } else {

      setUser(null);
      setLoading(false);

    }

  }, [token]);


  /*
   * LOGIN
   */
  const login = async (jwt) => {

    localStorage.setItem(
      "stockly_token",
      jwt
    );

    setToken(jwt);

    setLoading(true);

    try {

      const response = await api.get(
        "/users/me"
      );

      setUser(response.data);

    } catch (error) {

      console.error(
        "Failed to load user after login:",
        error
      );

      setUser(null);

    } finally {

      setLoading(false);

    }
  };


  /*
   * LOGOUT
   */
  const logout = () => {

    localStorage.removeItem(
      "stockly_token"
    );

    setToken(null);

    setUser(null);

  };


  return (
    <AuthContext.Provider
      value={{
        token,
        user,
        loading,
        login,
        logout,
        isAuthenticated: !!token,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}


export function useAuth() {

  return useContext(AuthContext);

}