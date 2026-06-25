import urllib.request
import urllib.error
import json

data = json.dumps({
    "name": "Test",
    "email": "test@test.com",
    "cpf": "03205602101"
}).encode('utf-8')

req = urllib.request.Request("http://localhost:8080/api/person", data=data, headers={"Content-Type": "application/json"})
try:
    with urllib.request.urlopen(req) as res:
        print("Status:", res.status)
        print("Body:", res.read().decode('utf-8'))
except urllib.error.HTTPError as e:
    print("Status:", e.code)
    print("Headers:", e.headers)
    print("Body:", e.read().decode('utf-8'))
