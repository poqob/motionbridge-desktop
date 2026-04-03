user=$(logname || echo $SUDO_USER)
echo "Target user: $user"
su - $user -c "pactl set-sink-volume @DEFAULT_SINK@ 50%"
