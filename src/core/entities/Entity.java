package core.entities;

import java.awt.*;

public abstract class Entity {
    protected float x, y;
    protected float width, height;
    protected boolean active = true;
    
    public Entity(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    public abstract void update(double delta);
    public abstract void render(Graphics2D g);
    
    public Rectangle getBounds() {
        return new Rectangle((int)x, (int)y, (int)width, (int)height);
    }
    
    public boolean collidesWith(Entity other) {
        return getBounds().intersects(other.getBounds());
    }
    
    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public float getCenterX() { return x + width / 2; }
    public float getCenterY() { return y + height / 2; }
}
