import paramiko
client=paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect('100.96.171.43', username='serius', password='kecilsemua')
stdin,stdout,stderr=client.exec_command('cat /var/www/study-center-nias/resources/js/app.js')
print(stdout.read().decode())
client.close()
