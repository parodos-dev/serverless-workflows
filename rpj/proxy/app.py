import os
import logging
from flask import Flask, request, Response
import requests

app = Flask(__name__)

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Get the target URL from environment variables
TARGET_URL = os.getenv('TARGET_URL')

@app.route('/health', methods=['GET'])
def health_check():
    return Response("Healthy", status=200)

@app.route('/<path:url>', methods=['GET', 'POST', 'PUT', 'DELETE'])
def proxy(url):
    # Construct the full target URL
    target = f"{TARGET_URL}/{url}"
    
    # Log the request
    logger.info(f"Proxying request to: {target}")
    
    # Forward the request
    try:
        # Forward the request based on the HTTP method
        if request.method == 'GET':
            resp = requests.get(target, params=request.args, verify=False)
        elif request.method == 'POST':
            resp = requests.post(target, json=request.get_json(), verify=False)
        elif request.method == 'PUT':
            resp = requests.put(target, json=request.get_json(), verify=False)
        elif request.method == 'DELETE':
            resp = requests.delete(target, verify=False)
        else:
            return Response("Method Not Allowed", status=405)

        content = resp.content
        logger.debug(f"Content:\n{content}")
        # Return the response from the target server
        return Response(content, status=resp.status_code, content_type=resp.headers['Content-Type'])

    except requests.exceptions.RequestException as e:
        logger.error(f"Error while forwarding request: {e}")
        return Response("Service Unavailable", status=503)

if __name__ == "__main__":
    app.run(host='0.0.0.0', port=8080)
