<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<html>
  <head>
    <title>City Directory</title>
    <style>
    #stylized label{
    font-size:18px;
    font-weight:bold;
    text-align:right;
    }
    #stylized input{
    font-size:16px;
    padding:4px 2px;
    border:solid 1px #aacfe4;
    width:200px;
    margin:2px 0 20px 10px;
    display: block;
    }
    </style>
  </head>
  <body>
    <div id="stylized" class="default">
      <form id="form" name="form" method="get" action="suggestions">
        <h1>Enter Query</h1>
        <label>Name: </label>
        <input type="text" name="q" id="name"/>
        <label>Latitude: </label>
        <input type="text" name="latitude" id="lat"/>
        <label>Longitude: </label>
        <input type="text" name="longitude" id="long"/>
        <button type="submit">Search</button>
      </form>
    </div>
  </body>
</html>
