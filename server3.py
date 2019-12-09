from http.server import HTTPServer, BaseHTTPRequestHandler
import json as js
import sys
import base64 as decoder
from PIL import Image
import random 
import base64

class RequestHandler(BaseHTTPRequestHandler):
  def _set_headers(self):
        self.send_response(200)
        self.send_header("Content-type", "application/json")
        self.end_headers()
#        self.wfile.write(bytes(js.dumps({'ok': 'ok'}, ensure_ascii=False), 'utf-8'))

  def do_POST(self):
    content_length = int(self.headers['Content-Length']) # <--- Gets the size of data
    post_data=self.rfile.read(content_length)
    data= js.loads(post_data.decode('utf-8'))
    f=open('readings.txt','a')
    image_width=int(data["y"])
    image_height=int(data["x"])
    image_data=base64.decodestring(bytes(data["image"],'UTF-8'))
    f.write(str(image_width)+" "+str(image_height)+" \n")
    g=open('tmp'+str(random.random())+".jpg",'ab')
    g.write(image_data)
    g.close()
    f.close()
    self.send_response(200)
    self.send_header("Content-type", "application/json")
    self.end_headers()
    self.wfile.write(bytes(js.dumps({'response': 'ok'}, ensure_ascii=False), 'utf-8'))
    #self._set_headers()

  def do_GET(self):
    self.send_response(200)
   
    #self.send_header("Content-type", "image/jpeg")
    #self.end_headers()
    
    #self.send_header("Content-type", "multipart/x-mixed-replace; boundary=frame")
    self.send_header("MIME-Version", "1.0")
    #self.wfile.write(bytes("MIME-Version: 1.0\r\n",'UTF-8'))
    self.send_header("Content-type", "multipart/mixed; boundary=frame")
    self.end_headers()
    
    self.wfile.write(bytes("--frame\r\n",'UTF-8'))
    self.wfile.write(bytes("Content-type: text/plain\r\n",'UTF-8'))
    self.wfile.write(bytes("prova..",'UTF-8'))
 #   self.wfile.write(bytes("Content-type: image/jpeg\r\n\r\n",'UTF-8'))
 #   g=open("test.jpg",'rb')
 #   c = g.read() 
 #   g.close()
 #   self.wfile.write(c)
    self.wfile.write(bytes("\r\n--frame--",'UTF-8'))



if __name__ == "__main__":
   ip=sys.argv[1]
   port=8000
   server = HTTPServer((ip, port), RequestHandler)
   print('Server started on ip',ip,'port',port,'waiting....')
   server.serve_forever()
