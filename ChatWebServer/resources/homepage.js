"use strict";


//////////////////   CLIENT SIDE  //////////////////
//  - let the user to enter a room
//  - let the user to send messages in this room
//  - print the history of messages in this room for user who just enters the room
//  - let the user to check who else are currently in this room
//  - let the user to check what rooms are currently exists



// handle the call back messages from the server
function handleMessageCB(event){
    console.log(event);
    let result = event.data;
    let resultObj = JSON.parse(result);
    console.log("result: " + result);
    // if the callback message is a 'message' sent by clients
    if ('message' in resultObj) {
        let time = resultObj.time;
        let name = resultObj.user;
        let message = resultObj.message;
        let chatLine = document.createElement("p");
        let mark = document.createElement("mark");
        let chatLineText = document.createTextNode("[ " + time + " ]  " + name + ": " + message);
        mark.appendChild(chatLineText);
        chatLine.appendChild(mark);
        messages.appendChild(chatLine);
        messages.scrollTop = messages.scrollHeight - messages.clientHeight;
    }
    // if the callback message is the list of users in current room
    else if ('usersList' in resultObj){
        let roomName = resultObj.roomName;
        let roomLine = document.createElement("p");
        let roomLineText = document.createTextNode(">>>>>>>> Users who are currently in room '" + roomName + "': ");
        roomLine.appendChild(roomLineText);
        messages.appendChild(roomLine);
        let userList = resultObj.usersList;
        let users= userList.split(" ");
        for (let user of users){
            let userLine = document.createElement("p");
            let userLineText = document.createTextNode(" * " + user);
            userLine.appendChild(userLineText);
            messages.appendChild(userLine);
        }
        let blankLine = document.createElement("p");
        let blankLineText = document.createTextNode(">>>>>>>>");
        blankLine.appendChild(blankLineText);
        messages.appendChild(blankLine);
    }
    // if the callback message is the list of rooms
    else if ('roomList' in resultObj){
        let roomLine = document.createElement("p");
        let roomLineText = document.createTextNode(">>>>>>>> Currently existed rooms:");
        roomLine.appendChild(roomLineText);
        messages.appendChild(roomLine);
        let roomList = resultObj.roomList;
        let rooms = roomList.split(" ");
        for (let room of rooms){
            let roomLine = document.createElement("p");
            let roomLineText = document.createTextNode(" * " + room);
            roomLine.appendChild(roomLineText);
            messages.appendChild(roomLine);
        }
        let blankLine = document.createElement("p");
        let blankLineText = document.createTextNode(">>>>>>>>");
        blankLine.appendChild(blankLineText);
        messages.appendChild(blankLine);
    }
}


// handle web socket call back errors
function handleErrorCB(){
    alert("A WebSocket error occurred when connecting...");
}


// handle the server closed situation
function handleCloseCB(){
    enter.value = "Server Closed.";
}

// connect the user to a room
function StartConnect() {
    // when user press 'enter room' button
    // get all info the user enters
    let userName = userNameEle.value;
    let roomName = roomNameEle.value;
    for (let i=0; i<roomName.length; i++){
        if ( roomName[i] < 'a' || roomName[i] > 'z'){
            alert("Invalid room name.");
            roomNameEle.value = "need to be lowercase";
            return -1;
        }
    }
    let connectJSON = "join " + userName + " " + roomName; // rule: requestType userName roomName
    console.log(connectJSON);

    // send to the server
    ws.send(connectJSON);
    let roomInfo = document.createElement("p");
    roomInfo.classList.add("rooms");
    let roomText = document.createTextNode("Welcome! You are now in the room: '" + roomName + "'.");
    roomInfo.appendChild(roomText);
    messages.appendChild(roomInfo);
    console.log(roomText);
}


// send messages to the current room
function SendMessage() {
    let enterText = enter.value;
    if (enterText === ""){
        alert("Invalid message.");
        return -1;
    }
    let messageJSON = "message " + userNameEle.value + " " + enterText; // rule: requestType userName roomName
    console.log(messageJSON);
    ws.send(messageJSON);
    enter.value="";
}


// when a user press 'return' on her keyboard, send the message
function HandleKeyPress(event){
    if (event.keyCode === 13){
        event.preventDefault();
        SendMessage();
    }
}


// when the client request the users list of current room, send request to the server
function reqeustUserList() {
    let userName = userNameEle.value;
    let roomName = roomNameEle.value;
    let json = "requestUsersList " + userName + roomName;
    ws.send(json);
}


// when the client request the list of existed rooms, send request to the server
function requestRoomList() {
    let userName = userNameEle.value;
    let roomName = roomNameEle.value;
    let json = "requestRoomList " + userName + roomName;
    ws.send(json);
}


///////////////////////////////////////////////////
/////////////////// MAIN PART /////////////////////


// CREATE ELEMENTS OF INPUTS: USERNAME + ROOMNUM
let userNameEle = document.getElementById("nickname");
let roomNameEle = document.getElementById("roomName");
let messages = document.getElementById("messageBoard");

// CREATE THE WEB SOCKET
let ws = new WebSocket("ws://localhost:8081/resources");

// HANDLE CONNECTION TO THE SERVER
ws.onopen = function(event){
    console.log(event);
    console.log("WebSocket is now connected to the server...");
}

// HANDLE WEB SOCKET ACTIVITIES
ws.onmessage = handleMessageCB;
ws.onerror = handleErrorCB;
ws.onclose = handleCloseCB;

// WHEN HIT THE BUTTON, SEND MESSAGE TO THE SERVER TO JOIN A CHAT ROOM
let button = document.getElementById("loginButton");
button.addEventListener("click", StartConnect);

// COLLECT THE CONTENT OF THE USER'S MESSAGE, AND LISTEN TO THE BUTTON AND ENTER
let enter = document.getElementById("type");
let sendBtn = document.getElementById("send");
enter.addEventListener("keypress", HandleKeyPress);
sendBtn.addEventListener("click", SendMessage);

// ADD FEATURES: BUTTON 'ROOM INFO' REQUESTS THE USER LIST OF CURRENT ROOM
let roomInfoBtn = document.getElementById("roomInfo");
roomInfoBtn.addEventListener("click", reqeustUserList);

// ADD FEATURES: BUTTON 'ROOM LIST' REQUESTS THE LIST OF CURRENT EXISTED ROOMS
let roomListBtn = document.getElementById("roomList");
roomListBtn.addEventListener("click", requestRoomList);