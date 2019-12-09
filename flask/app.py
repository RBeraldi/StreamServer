from flask import Flask, render_template, Response , request
import base64
import threading

outputFrame = None
lock = threading.Lock()

app = Flask(__name__)

@app.route('/')
def index():
    return render_template('index.html')


def get_frame():
	global lock,outputFrame
	with lock:
		if outputFrame is None:
			g=open("test.jpg",'rb')
			outputFrame=g.read(image)
			g.close()
		else:
			return outputFrame

def gen():
    while True:
        frame = get_frame()
        yield (b'--frame\r\n'
               b'Content-Type: image/jpeg\r\n\r\n' + frame + b'\r\n')

@app.route('/video_feed')
def video_feed():
    return Response(gen(),
                    mimetype='multipart/x-mixed-replace; boundary=frame')

@app.route('/', methods=['POST'])
def post():
	global lock,outputFrame
	data = request.get_json()
	image=base64.decodestring(bytes(data["image"],'UTF-8'))
	with lock:
		outputFrame=image
		return {"reply": "ok"}, 200
#	g=open("test.jpg",'wb')
#	g.write(image)
#	g.close()
