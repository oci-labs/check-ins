import React, { useContext, useEffect, useState } from "react";
import {useHistory} from "react-router-dom";
import { getMembersByPDL } from "../../api/member";
import { getCheckinByMemberId } from "../../api/checkins";
import { AppContext } from "../../context/AppContext";
import { UPDATE_CHECKINS } from "../../context/actions";
import { selectCurrentUserId, selectMostRecentCheckin, selectCsrfToken } from "../../context/selectors";
import Card from '@material-ui/core/Card';
import CardHeader from '@material-ui/core/CardHeader';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemText from '@material-ui/core/ListItemText';
import ListItemAvatar from '@material-ui/core/ListItemAvatar';
import GroupIcon from "@material-ui/icons/Group";
import Avatar from "../avatar/Avatar"
import { getAvatarURL } from "../../api/api.js";

import "./Personnel.css";

const Personnel = () => {
  const { state, dispatch } = useContext(AppContext);
  const history = useHistory();
  const csrf = selectCsrfToken(state);
  const id = selectCurrentUserId(state);
  const [personnel, setPersonnel] = useState();

  // Get personnel
  useEffect(() => {
    async function updatePersonnel() {
      if (id) {
        let res = await getMembersByPDL(id, csrf);
        let data =
          res.payload &&
          res.payload.data &&
          res.payload.status === 200 &&
          !res.error
            ? res.payload.data
            : null;
        if (data) {
          setPersonnel(data);
        }
      }
    }
    if (csrf) {
      updatePersonnel();
    }
  }, [csrf, id]);

  // Get checkins per personnel
  useEffect(() => {
    async function updateCheckins() {
      if (personnel) {
        for (const person of personnel) {
          let res = await getCheckinByMemberId(person.id, csrf);
          let data =
            res && res.payload && res.payload.status === 200
              ? res.payload.data
              : null;
          if (data && data.length > 0 && !res.error) {
            dispatch({type: UPDATE_CHECKINS, payload: data});
          }
        }
      }
    }
    if (csrf) {
      updateCheckins();
    }
  }, [csrf, personnel, dispatch]);

  // Create entry of member and their last checkin
  function createEntry(person, lastCheckin, keyInput) {
    let key = keyInput ? keyInput : undefined;
    let name = "Team Member";
    let workEmail = "";
    let lastCheckInDate = "Unknown";
    if(lastCheckin?.checkInDate) {
      const [year, month, day, hour, minute] = lastCheckin.checkInDate;
      lastCheckInDate = new Date(year, month - 1, day, hour, minute, 0).toLocaleDateString();
    }

    if (person) {
      let id = person.id ? person.id : null;
      name = person.name ? person.name : id ? id : name;
      workEmail = person.workEmail;
      key = id && !key ? `${id}Personnel` : key;
    }

    return (
      <ListItem key={key} button
          onClick={() => {
            history.push(`/checkins/${person?.id}`);
          }}
      >
        <ListItemAvatar>
          <Avatar
            alt={name}
            src={getAvatarURL(workEmail)}
          />
        </ListItemAvatar>
        <ListItemText primary={name} secondary={lastCheckInDate}/>
      </ListItem>
    );
  }

  // Create the entries for the personnel container
  const createPersonnelEntries = () => {
    if (personnel && personnel.length > 0) {
      return personnel.map((person) => createEntry(person, selectMostRecentCheckin(state, person.id), null));
    } else {
      return [];
    }
  };

  return (
    <Card>
      <CardHeader avatar={<GroupIcon />} title="Development Partners" />
        <List dense>
          {createPersonnelEntries()}
        </List>
    </Card>
  );
};

export default Personnel;
