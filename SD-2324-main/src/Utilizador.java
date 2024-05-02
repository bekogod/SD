


public class Utilizador {
    private String username;
    private String password;
    private Boolean status;

    
    public Utilizador(String username, String password){
        this.username = username;
        this.password = password;
        //assume se que quando se cria um atualizador ele fica logo online e segue para o menu
        this.status = true;
    }


    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getStatus() {
        return this.status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }
    
}
