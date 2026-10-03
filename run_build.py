import paramiko
client=paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect('100.96.171.43', username='serius', password='kecilsemua')
stdin,stdout,stderr=client.exec_command('cd /var/www/study-center-nias && npm run build')
print(stdout.read().decode())
print(stderr.read().decode())
client.close()
