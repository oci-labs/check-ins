import React, { useState } from "react";

import "./SkillSlider.css"

import {
    Checkbox,
    FormControl,
    FormControlLabel,
    IconButton
} from '@material-ui/core';
import DeleteIcon from "@material-ui/icons/Delete";
import TextField from '@material-ui/core/TextField';
import { debounce } from "lodash/function";
import { makeStyles } from "@material-ui/core/styles";
import DiscreteSlider from '../slider/Slider'

const useStyles = makeStyles((theme) => ({
  hidden: {
    display: "none",
  }
}));

const SkillSlider = ({id, name, startLevel, lastUsedDate, onDelete, onUpdate, index, key}) => {
    let [currCheck, setCurrCheck] = useState(!lastUsedDate);
    let [lastUsed, setLastUsed] = useState(lastUsedDate);
    let [skillLevel, setSkillLevel] = useState(startLevel);

    const classes = useStyles();

    const datePickerVisibility = (event) => {
      setCurrCheck(!currCheck);
    };

    const updateLevel = (e, value) => setSkillLevel(value);

    const updateSkillLevel = debounce((event, value) => {
        onUpdate(lastUsed, value, index);
    }, 1500);

    const updateLastUsed = debounce((value) => {
        setLastUsed(value)
        onUpdate(value, skillLevel, index);
    }, 1500);

    const formatDate = (date) => {
        if (!date) {
            return;
        }
        let dateString = date[0] + "-";
        dateString = dateString + (date[1] < 10 ? "0" + date[1] : date[1]) + "-";
        dateString = dateString + (date[2] < 10 ? "0" + date[2] : date[2]);
        return dateString;
    }

    return (
      <React.Fragment>
        <DiscreteSlider title={name} inStartPos={skillLevel} onChange={updateLevel} onChangeCommitted={updateSkillLevel}/>
        {false && <FormControl>
          <FormControlLabel
            control={<Checkbox color="primary" value="current" />}
            label='Currently Used'
            labelPlacement="top"
            checked={currCheck}
            onChange={datePickerVisibility}
          />
        </FormControl>}
        {false && <TextField
          className={currCheck ? classes.hidden: undefined}
          type="date"
          onChange={(event, value) => updateLastUsed(value)}
          defaultValue={formatDate(lastUsed)}
        />}
        <IconButton onClick={(event) => onDelete(id)}>
          <DeleteIcon />
        </IconButton>
      </React.Fragment>
    );
};
export default SkillSlider;