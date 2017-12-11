echo "Stopping play application process..."

sudo service heilsusaga-play stop
unzip -o /var/lib/jenkins/workspace/Heilsusaga-dev/target/universal/heilsusaga-1.0.zip -d /websites/is.loftfar.tf.heilsusaga/play
sleep 1
echo "Starting play application process..."
sudo service heilsusaga-play start
echo "Deployment completed"

