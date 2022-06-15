/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
using System;
using System.Collections;
using System.Collections.Generic;
using System.Security.Cryptography;
using System.Text;
using UnityEditor.UIElements;
using UnityEngine;
using UnityEngine.Networking;
using UnityEngine.UI;
using UnityEngine.UIElements;

public class PlayIntegrityController : MonoBehaviour
{
    public GameObject expressButton;
    [SerializeField] private Text randomLabel;
    [SerializeField] private Text resultLabel;

    private const string TEST_COMMAND = "TRANSFER FROM alice TO bob CURRENCY gems QUANTITY 1000";
    private const string URL_GETRANDOM = "https://your-play-integrity-server.com/getRandom";
    private const string URL_PERFORMCOMMAND = "https://your-play-integrity-server.com/performCommand";
    private const string CONTENT_TYPE = "Content-Type";
    private const string JSON_CONTENT = "application/json";

    // The random number returned from the server to help construct
    // a nonce value for a Play Integrity request
    private string _randomString = "";

    // The 'express' token returned from the server after a successful command,
    // permitting a subsequent command to be submitted using an express token
    // instead of a Play Integrity token. Express tokens are single-use, but
    // a successful express token command returns a new express token.
    private string _expressToken = "";

    // Class to serialize the getRandom response Json, using
    // the UnityEngine.JsonUtility class.
    [Serializable]
    public class RandomResult
    {
        public string random;
        public ulong timestamp;
    }

    public void StartRandomRequest()
    {
        Debug.Log("StartRandomRequest called");
        StartCoroutine(GetRandomRequest());
    }

    public void StartIntegrityCommand()
    {
        Debug.Log("StartIntegrityCommand called");
        StartCoroutine(RunIntegrityCommand());
    }

    public void StartExpressCommand()
    {
        Debug.Log("StartExpressCommand called");
        StartCoroutine(RunExpressCommand());
    }

    // Create a nonce string, containing two parts:
    // 1) A random number retrieved from the server
    // 2) A SHA-256 hash of the command string being passed to the server
    // Both values are byte arrays converted into hex strings. The Play Integrity
    // API expects the nonce string to be URL encoded Base64 with no padding. The
    // hex string is a valid Base64 string, even though we don't actually
    // encode or decode it.
    string GenerateNonceString(string randomString, string commandString)
    {
        using SHA256Managed sha256 = new SHA256Managed();
        byte[] commandHashBytes = sha256.ComputeHash(
            System.Text.Encoding.UTF8.GetBytes(commandString));
        StringBuilder nonceStringBuilder = new StringBuilder(
            randomString.Length + (commandHashBytes.Length * 2));
        nonceStringBuilder.Append(randomString);
        foreach (byte b in commandHashBytes)
        {
            nonceStringBuilder.Append(b.ToString("x2"));
        }

        return nonceStringBuilder.ToString();
    }

    IEnumerator RunIntegrityCommand()
    {
        yield return null;
    }

    IEnumerator RunExpressCommand()
    {
        if (!string.IsNullOrEmpty(_expressToken))
        {
            // Send the command to our server with a POST request, including the
            // express token, which the server will validate against its list
            // of generated express tokens
            yield return PostServerCommand(_expressToken);
        }
    }

    IEnumerator PostServerCommand(string tokenResponse)
    {
        yield return null;
    }

    IEnumerator GetRandomRequest()
    {
        // Start a HTTP GET request to the getRandom URL
        var randomRequest = new UnityWebRequest(URL_GETRANDOM, "GET");
        randomRequest.downloadHandler = (DownloadHandler) new DownloadHandlerBuffer();
        randomRequest.SetRequestHeader(CONTENT_TYPE, JSON_CONTENT);
        yield return randomRequest.SendWebRequest();

        if (randomRequest.result == UnityWebRequest.Result.Success)
        {
            var result = JsonUtility.FromJson<RandomResult>(randomRequest.downloadHandler.text);
            if (result != null)
            {
                _randomString = result.random;
                var nonceString = GenerateNonceString(_randomString, TEST_COMMAND);
                Debug.Log(nonceString);
            }
            else
            {
                Debug.Log("Invalid random json");
                _randomString = "";
            }
        }
        else
        {
            Debug.Log($"Web request error on processToken: {randomRequest.error}");
            _randomString = "";
        }
        randomLabel.text = _randomString;
    }
}
