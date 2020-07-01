import React, { useContext } from "react";
import ProfileContext from "../../context/ProfileContext";
import "./Profile.css";

const Profile = () => {
  const context = useContext(ProfileContext);
  const { bio, email, image_url, name, pdl, role } = context.defaultProfile;

  return (
    <div className="flex-row" style={{ marginTop: "20px" }}>
      <div className="profile-image">
        <img
          alt="Profile"
          src={image_url ? image_url : "https://i.imgur.com/TkSNOpF.jpg"}
        />
      </div>
      <div className="flex-row">
        <div style={{ textAlign: "left" }}>
          <h2 style={{ margin: 0 }}>{name}</h2>
          <div>
            <p>Role: {role}</p>
            <p>Email: {email}</p>
            <p>Current PDL: {pdl}</p>
            <p>{bio}</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Profile;