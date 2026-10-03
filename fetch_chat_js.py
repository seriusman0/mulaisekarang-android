import paramiko
client=paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect('100.96.171.43', username='serius', password='kecilsemua')
sftp = client.open_sftp()
sftp.get('/var/www/study-center-nias/resources/js/chat.js', 'chat.js')
sftp.close()
client.close()