import axios from "axios";

export const $ = axios.create({
  baseURL: "http://api.allback.site/",
  headers: {
    "Content-Type": "application/json; charset=UTF-8",
  },
});

export const $_admin = axios.create({
  // baseURL: "http://k8a806.p.ssafy.io:8090/api/v1",
  baseURL: "http://api.allback.site/",
  headers: {
    "Content-Type": "application/json; charset=UTF-8",
  },
});

$.interceptors.request.use((config) => {
  config.headers["Authorization"] = `Bearer ${JSON.parse(
    sessionStorage.getItem("accessToken")
  )}`;
  return config;
});

$_admin.interceptors.request.use((config) => {
  config.headers["Authorization"] = `Bearer ${JSON.parse(
    sessionStorage.getItem("accessToken")
  )}`;
  return config;
});
