import axios from "axios";
import { resolve, BASE_API_URL } from "./api.js";

export const getSkills = async () => {
  return await resolve(
    axios({
      method: "get",
      url: `${BASE_API_URL}/services/skill/?pending=false`,
      responseType: "json",
      auth: {
        username: "ADMIN",
        password: "ADMIN",
      },
    })
  );
};

export const getSkillById = async (id) => {
  return await resolve(
    axios({
      method: "get",
      url: `${BASE_API_URL}/services/skill/${id}`,
      responseType: "json",
      auth: {
        username: "ADMIN",
        password: "ADMIN",
      },
    })
  );
};

export const createSkill = async (skill) => {
  return await resolve(
    axios({
      method: "post",
      url: `${BASE_API_URL}/services/skill`,
      responseType: "json",
      data: skill,
      auth: {
        username: "ADMIN",
        password: "ADMIN",
      },
    })
  );
};
